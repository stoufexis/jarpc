package stoufexis.jarpc.exchange;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.BaseCallback;
import stoufexis.jarpc.model.BaseCatalog;
import stoufexis.jarpc.model.DecodeFailureResponse;
import stoufexis.jarpc.model.JarpcClient;

import static stoufexis.jarpc.util.Util.*;

public class JarpcExchangeClient extends JarpcClient implements ExchangeClient {
  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.
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

  private final BufferClaim claim = new BufferClaim();

  @Override
  public void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback) {
    if (postOrderCallbacks.putIfAbsent(correlationId, callback) != null) {
      callback.onDuplicateId(correlationId);
      return;
    }

    long result = publication.tryClaim(request.getMessageSize(), claim);

    if (result < 0) {
      postOrderCallbacks.remove(correlationId);
      interpretError(result, correlationId, callback);
      return;
    }

    try {
      request.encode(claim.buffer(), claim.offset());
      claim.commit();
    } catch (RuntimeException e) {
      claim.abort();
      postOrderCallbacks.remove(correlationId);
      interpretError(result, correlationId, callback);
      throw e;
    }
  }

  @Override
  public void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback) {
    if (cancelAllCallbacks.putIfAbsent(correlationId, callback) != null) {
      callback.onDuplicateId(correlationId);
      return;
    }

    long result = publication.tryClaim(request.getMessageSize(), claim);

    if (result < 0) {
      cancelAllCallbacks.remove(correlationId);
      interpretError(result, correlationId, callback);
      return;
    }

    try {
      request.encode(claim.buffer(), claim.offset());
      claim.commit();
    } catch (RuntimeException e) {
      claim.abort();
      cancelAllCallbacks.remove(correlationId);
      interpretError(result, correlationId, callback);
      throw e;
    }
  }

  // Subscription
  //

  private final PostOrderResponse postOrderResponse = new PostOrderResponse();
  private final CancelAllResponse cancelAllResponse = new CancelAllResponse();
  private final DecodeFailureResponse decodeFailureResponse = new DecodeFailureResponse();

  @Override
  protected boolean handleReceivedFragment(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {

    switch (messageType) {
      case BaseCatalog.decodeFailure -> {
        decodeFailureResponse.decode(buffer, offset, length);

        int baseMessageType = decodeFailureResponse.getBaseMessageType();

        switch (baseMessageType) {
          case Catalog.postOrderId ->
              removeOrThrow(postOrderCallbacks, correlationId)
                  .onServerDecodeError(
                      correlationId,
                      decodeFailureResponse.getBytes(),
                      decodeFailureResponse.getBytesSize());

          case Catalog.cancelAllOrdersId ->
              removeOrThrow(cancelAllCallbacks, correlationId)
                  .onServerDecodeError(
                      correlationId,
                      decodeFailureResponse.getBytes(),
                      decodeFailureResponse.getBytesSize());

          default -> {
            handler.onError(illegal("Unknown message type " + baseMessageType));
            return true;
          }
        }

        return true;
      }

      case Catalog.postOrderId -> {
        PostOrderCallback callback = removeOrThrow(postOrderCallbacks, correlationId);

        try {
          postOrderResponse.decode(buffer, offset, length);
          return callback.onResponse(correlationId, postOrderResponse);

        } catch (RuntimeException e) {
          callback.onClientDecodeError(correlationId, e);
          return true;
        }
      }

      case Catalog.cancelAllOrdersId -> {
        CancelAllCallback callback = removeOrThrow(cancelAllCallbacks, correlationId);

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
}
