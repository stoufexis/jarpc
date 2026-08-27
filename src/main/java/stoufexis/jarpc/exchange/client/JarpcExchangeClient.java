package stoufexis.jarpc.exchange.client;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.client.JarpcClient;
import stoufexis.jarpc.model.MessageHeader;

import static stoufexis.jarpc.util.Util.*;

public final class JarpcExchangeClient extends JarpcClient implements ExchangeClient {
  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.

  // FIXME requests should timeout after a while of inactivity

  private final NonBlockingHashMapLong<PostOrderCallback> postOrderCallbacks =
      new NonBlockingHashMapLong<>();

  private final NonBlockingHashMapLong<CancelAllCallback> cancelAllCallbacks =
      new NonBlockingHashMapLong<>();

  private final Publication publication;

  JarpcExchangeClient(Publication publication, Subscription subscription, ErrorHandler handler) {
    super(publication, subscription, handler);
    this.publication = publication;
  }

  public static JarpcExchangeClient create(
      Aeron aeron,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId,
      ErrorHandler handler) {
    Subscription sub = createClientSubscription(aeron, responseControl, responseStreamId);
    Publication pub = createClientPublication(aeron, requestEndpoint, requestStreamId, sub);
    return new JarpcExchangeClient(pub, sub, handler);
  }

  // Publication
  //

  @Override
  public ErrorCode postOrder(
      long correlationId, PostOrderRequest request, PostOrderCallback callback) {

    if (postOrderCallbacks.putIfAbsent(correlationId, callback) != null) {
      return ErrorCode.DUPLICATE_ID;
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
      return null;
    } catch (RuntimeException e) {
      claim.abort();
      postOrderCallbacks.remove(correlationId);
      return ErrorCode.ENCODE_ERROR;
    }
  }

  @Override
  public ErrorCode cancelAll(
      long correlationId, CancelAllRequest request, CancelAllCallback callback) {

    if (cancelAllCallbacks.putIfAbsent(correlationId, callback) != null) {
      return ErrorCode.DUPLICATE_ID;
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
      return null;
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
      long correlationId,
      boolean last,
      DirectBuffer buffer,
      int offset,
      int length) {

    switch (messageType) {
      case Catalog.postOrderId -> {
        PostOrderCallback callback = getCallbackOrThrow(postOrderCallbacks, correlationId, last);

        try {
          postOrderResponse.decode(buffer, offset, length);
          return callback.onResponse(correlationId, last, postOrderResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      case Catalog.cancelAllId -> {
        CancelAllCallback callback = getCallbackOrThrow(cancelAllCallbacks, correlationId, last);

        try {
          cancelAllResponse.decode(buffer, offset, length);
          return callback.onResponse(correlationId, last, cancelAllResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      default -> throw illegal("Unknown message type " + messageType);
    }
  }

  @Override
  protected void handleProcessingFailureResponse(int messageType, long correlationId, boolean last) {

    switch (messageType) {
      case Catalog.postOrderId ->
          getCallbackOrThrow(postOrderCallbacks, correlationId, last)
              .onServerDecodeError(correlationId);

      case Catalog.cancelAllId ->
          getCallbackOrThrow(cancelAllCallbacks, correlationId, last)
              .onServerDecodeError(correlationId);

      default -> throw illegal("Unknown message type " + messageType);
    }
  }
}
