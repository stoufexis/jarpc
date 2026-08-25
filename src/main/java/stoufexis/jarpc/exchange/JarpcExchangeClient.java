package stoufexis.jarpc.exchange;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.BaseCallback;
import stoufexis.jarpc.model.JarpcClient;
import stoufexis.jarpc.util.MessageHeader;

import static stoufexis.jarpc.util.Util.*;

public class JarpcExchangeClient extends JarpcClient implements Exchange {
  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.
  private final NonBlockingHashMapLong<PostOrderCallback> postOrderCallbacks =
      new NonBlockingHashMapLong<>();

  private final NonBlockingHashMapLong<CancelAllCallback> cancelAllCallbacks =
      new NonBlockingHashMapLong<>();

  private final MessageHeader header = new MessageHeader();
  private final PostOrderResponse postOrderResponse = new PostOrderResponse();
  private final CancelAllResponse cancelAllResponse = new CancelAllResponse();
  private final BufferClaim claim = new BufferClaim();

  public JarpcExchangeClient(
      Publication publication, Subscription subscription, ErrorHandler handler) {
    super(publication, subscription, handler);
  }

  @Override
  public void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback) {
    if (postOrderCallbacks.putIfAbsent(correlationId, callback) != null) {
      callback.onError(correlationId, BaseCallback.ErrorType.DUPLICATE_ID, null);
      return;
    }

    long result = publication.tryClaim(request.getMessageSize(), claim);

    if (result < 0) {
      postOrderCallbacks.remove(correlationId);
      callback.onError(correlationId, interpretError(result), null);
      return;
    }

    try {
      request.encode(claim.buffer(), claim.offset());
      claim.commit();
    } catch (RuntimeException e) {
      claim.abort();
      postOrderCallbacks.remove(correlationId);
      callback.onError(correlationId, interpretError(result), null);
      throw e;
    }
  }

  @Override
  public void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback) {
    if (cancelAllCallbacks.putIfAbsent(correlationId, callback) != null) {
      callback.onError(correlationId, BaseCallback.ErrorType.DUPLICATE_ID, null);
      return;
    }

    long result = publication.tryClaim(request.getMessageSize(), claim);

    if (result < 0) {
      cancelAllCallbacks.remove(correlationId);
      callback.onError(correlationId, interpretError(result), null);
      return;
    }

    try {
      request.encode(claim.buffer(), claim.offset());
      claim.commit();
    } catch (RuntimeException e) {
      claim.abort();
      cancelAllCallbacks.remove(correlationId);
      callback.onError(correlationId, interpretError(result), null);
      throw e;
    }
  }

  @Override
  protected boolean handleReceivedFragment(DirectBuffer buffer, int offset, int length) {
    try {
      header.decode(buffer, offset, length);

      int messageType = header.getMessageType();
      long correlationId = header.getCorrelationId();

      offset += MessageHeader.HEADER_SIZE;
      length -= MessageHeader.HEADER_SIZE;

      switch (messageType) {
        case Catalog.postOrderId -> {
          PostOrderCallback callback = removeOrThrow(postOrderCallbacks, correlationId);

          try {
            postOrderResponse.decode(buffer, offset, length);
            return callback.onResponse(correlationId, postOrderResponse);

          } catch (RuntimeException e) {
            callback.onError(correlationId, BaseCallback.ErrorType.DECODE_ERROR, e);
            return true;
          }
        }

        case Catalog.cancelAllOrdersId -> {
          CancelAllCallback callback = removeOrThrow(cancelAllCallbacks, correlationId);

          try {
            cancelAllResponse.decode(buffer, offset, length);
            return callback.onResponse(correlationId, cancelAllResponse);

          } catch (RuntimeException e) {
            callback.onError(correlationId, BaseCallback.ErrorType.DECODE_ERROR, e);
            return true;
          }
        }

        default -> {
          handler.onError(illegal("Unknown message type " + messageType));
          return true;
        }
      }
    } catch (RuntimeException e) {
      handler.onError(e);
      return true;
    }
  }
}
