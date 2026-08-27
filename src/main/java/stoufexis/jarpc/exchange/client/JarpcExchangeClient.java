package stoufexis.jarpc.exchange.client;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.client.JarpcClient;
import stoufexis.jarpc.model.MessageHeader;
import stoufexis.jarpc.util.IntKeyContainer;

import static stoufexis.jarpc.util.Util.*;

/**
 * Multiple client instances for the same server using the same media driver produce undefined
 * behavior, unless different stream-ids are used.
 */
public final class JarpcExchangeClient extends JarpcClient implements ExchangeClient {
  // FIXME requests should timeout after a while of inactivity

  private final IntKeyContainer<PostOrderCallback> postOrderCallbacks;
  private final IntKeyContainer<CancelAllCallback> cancelAllCallbacks;
  private final Publication publication;

  JarpcExchangeClient(
      int maxInFlight, Publication publication, Subscription subscription, ErrorHandler handler) {
    super(publication, subscription, handler);
    this.publication = publication;
    this.postOrderCallbacks = new IntKeyContainer<>(maxInFlight);
    this.cancelAllCallbacks = new IntKeyContainer<>(maxInFlight);
  }

  public static JarpcExchangeClient create(
      int maxInFlight,
      Aeron aeron,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId,
      ErrorHandler handler) {
    Subscription sub = createClientSubscription(aeron, responseControl, responseStreamId);
    Publication pub = createClientPublication(aeron, requestEndpoint, requestStreamId, sub);
    return new JarpcExchangeClient(maxInFlight, pub, sub, handler);
  }

  // Publication
  //

  @Override
  public int postOrder(PostOrderRequest request, PostOrderCallback callback) {
    int correlationId = postOrderCallbacks.put(callback);
    if (ErrorCode.BACKPRESSURE == correlationId) {
      return ErrorCode.BACKPRESSURE;
    }

    MessageHeader header = request.getHeader();
    BufferClaim claim = request.getClaim();

    long result = publication.tryClaim(request.getMessageSize() + MessageHeader.HEADER_SIZE, claim);

    if (result < 0) {
      postOrderCallbacks.remove(correlationId);
      return interpretErrorCode(result);
    }

    try {
      header.set(correlationId, Catalog.postOrderId, true);
      header.encode(claim.buffer(), claim.offset());
      request.encode(claim.buffer(), claim.offset() + MessageHeader.HEADER_SIZE);
      claim.commit();
      return correlationId;
    } catch (RuntimeException e) {
      claim.abort();
      postOrderCallbacks.remove(correlationId);
      return ErrorCode.ENCODE_ERROR;
    }
  }

  @Override
  public int cancelAll(CancelAllRequest request, CancelAllCallback callback) {
    int correlationId = cancelAllCallbacks.put(callback);
    if (ErrorCode.BACKPRESSURE == correlationId) {
      return ErrorCode.BACKPRESSURE;
    }

    MessageHeader header = request.getHeader();
    BufferClaim claim = request.getClaim();

    long result = publication.tryClaim(request.getMessageSize() + MessageHeader.HEADER_SIZE, claim);

    if (result < 0) {
      cancelAllCallbacks.remove(correlationId);
      return interpretErrorCode(result);
    }

    try {
      header.set(correlationId, Catalog.cancelAllId, true);
      header.encode(claim.buffer(), claim.offset());
      request.encode(claim.buffer(), claim.offset() + MessageHeader.HEADER_SIZE);
      claim.commit();
      return correlationId;
    } catch (RuntimeException e) {
      claim.abort();
      cancelAllCallbacks.remove(correlationId);
      return ErrorCode.ENCODE_ERROR;
    }
  }

  // Subscription
  //

  private final PostOrderResponse postOrderResponse = new PostOrderResponse();
  private final CancelAllResponse cancelAllResponse = new CancelAllResponse();

  @Override
  protected boolean handleReceivedFragment(
      int messageType,
      int correlationId,
      boolean last,
      DirectBuffer buffer,
      int offset,
      int length) {

    switch (messageType) {
      case Catalog.postOrderId -> {
        PostOrderCallback callback = postOrderCallbacks.fetchOrThrow(correlationId, last);

        try {
          postOrderResponse.decode(buffer, offset, length);
          return callback.onResponse(last, correlationId, postOrderResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      case Catalog.cancelAllId -> {
        CancelAllCallback callback = cancelAllCallbacks.fetchOrThrow(correlationId, last);

        try {
          cancelAllResponse.decode(buffer, offset, length);
          return callback.onResponse(last, correlationId, cancelAllResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      default -> throw illegal("Unknown message type " + messageType);
    }
  }

  @Override
  protected void handleProcessingFailureResponse(int messageType, int correlationId, boolean last) {

    switch (messageType) {
      case Catalog.postOrderId ->
          postOrderCallbacks.fetchOrThrow(correlationId, last).onServerDecodeError(correlationId);

      case Catalog.cancelAllId ->
          cancelAllCallbacks.fetchOrThrow(correlationId, last).onServerDecodeError(correlationId);

      default -> throw illegal("Unknown message type " + messageType);
    }
  }
}
