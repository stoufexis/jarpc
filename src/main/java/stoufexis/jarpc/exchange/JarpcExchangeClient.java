package stoufexis.jarpc.exchange;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.JarpcClient;

import java.nio.ByteBuffer;

import static stoufexis.jarpc.util.Util.*;

public class JarpcExchangeClient extends JarpcClient implements ExchangeClient {
  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.

  // FIXME requests should timeout after a while

  private final NonBlockingHashMapLong<PostOrderCallback> postOrderCallbacks =
      new NonBlockingHashMapLong<>();

  private final NonBlockingHashMapLong<CancelAllCallback> cancelAllCallbacks =
      new NonBlockingHashMapLong<>();

  public JarpcExchangeClient(
      Publication publication, Subscription subscription, ErrorHandler handler) {
    super(publication, subscription, handler);
  }

  // Publication
  //

  @Override
  public ErrorCode postOrder(
      long correlationId, PostOrderRequest request, PostOrderCallback callback) {

    if (postOrderCallbacks.putIfAbsent(correlationId, callback) != null) {
      return ErrorCode.DUPLICATE_ID;
    }

    BufferClaim claim = request.getClaim();
    long result = publication.tryClaim(request.getMessageSize(), claim);

    if (result < 0) {
      postOrderCallbacks.remove(correlationId);
      return interpretErrorCode(result);
    }

    try {
      request.encode(claim.buffer(), claim.offset());
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

    BufferClaim claim = request.getClaim();
    long result = publication.tryClaim(request.getMessageSize(), claim);

    if (result < 0) {
      cancelAllCallbacks.remove(correlationId);
      return interpretErrorCode(result);
    }

    try {
      request.encode(claim.buffer(), claim.offset());
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
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {

    switch (messageType) {
      case Catalog.postOrderId -> {
        PostOrderCallback callback = removeCallbackOrThrow(postOrderCallbacks, correlationId);

        try {
          postOrderResponse.decode(buffer, offset, length);
          return callback.onResponse(correlationId, postOrderResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      case Catalog.cancelAllOrdersId -> {
        CancelAllCallback callback = removeCallbackOrThrow(cancelAllCallbacks, correlationId);

        try {
          cancelAllResponse.decode(buffer, offset, length);
          return callback.onResponse(correlationId, cancelAllResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      default -> {
        handler.onError(illegal("Unknown message type " + messageType));
        return true;
      }
    }
  }

  @Override
  protected void handleDecodeFailureResponse(
      int messageType, long correlationId, ByteBuffer bytes, int bytesSize) {

    switch (messageType) {
      case Catalog.postOrderId ->
          removeCallbackOrThrow(postOrderCallbacks, correlationId)
              .onServerDecodeError(correlationId, bytes, bytesSize);

      case Catalog.cancelAllOrdersId ->
          removeCallbackOrThrow(cancelAllCallbacks, correlationId)
              .onServerDecodeError(correlationId, bytes, bytesSize);

      default -> handler.onError(illegal("Unknown message type " + messageType));
    }
  }
}
