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
  // FIXME requests should support ad hoc aborts

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

    boolean accepted;

    switch (messageType) {
      case Catalog.postOrderId -> {
        PostOrderCallback callback = postOrderCallbacks.getOrThrow(correlationId);

        try {
          postOrderResponse.decode(buffer, offset, length);
        } catch (RuntimeException e) {
          accepted = callback.onClientDecodeError(correlationId, e);
          if (last && accepted) postOrderCallbacks.remove(correlationId);
          return accepted;
        }

        accepted = callback.onResponse(last, correlationId, postOrderResponse);
        if (last && accepted) postOrderCallbacks.remove(correlationId);
      }

      case Catalog.cancelAllId -> {
        CancelAllCallback callback = cancelAllCallbacks.getOrThrow(correlationId);

        try {
          cancelAllResponse.decode(buffer, offset, length);
        } catch (RuntimeException e) {
          accepted = callback.onClientDecodeError(correlationId, e);
          if (last && accepted) cancelAllCallbacks.remove(correlationId);
          return accepted;
        }

        accepted = callback.onResponse(last, correlationId, cancelAllResponse);
        if (last && accepted) cancelAllCallbacks.remove(correlationId);
      }

      default -> throw illegal("Unknown message type " + messageType);
    }

    return accepted;
  }

  @Override
  protected boolean handleProcessingFailureResponse(
      int messageType, int correlationId, boolean last) {

    boolean accepted;

    switch (messageType) {
      case Catalog.postOrderId -> {
        accepted = postOrderCallbacks.getOrThrow(correlationId).onServerDecodeError(correlationId);
        if (accepted && last) postOrderCallbacks.remove(correlationId);
      }

      case Catalog.cancelAllId -> {
        accepted = cancelAllCallbacks.getOrThrow(correlationId).onServerDecodeError(correlationId);
        if (accepted && last) cancelAllCallbacks.remove(correlationId);
      }

      default -> throw illegal("Unknown message type " + messageType);
    }

    return accepted;
  }
}
