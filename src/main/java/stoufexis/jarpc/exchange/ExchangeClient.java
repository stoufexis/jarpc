package stoufexis.jarpc.exchange;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.BaseCallback;
import stoufexis.jarpc.util.ClassAgent;
import stoufexis.jarpc.util.EventHandler;
import stoufexis.jarpc.util.MessageHeader;

import static stoufexis.jarpc.util.Util.illegal;

public class ExchangeClient implements Exchange {

  private final Publication publication;
  private final ReceiveAgent agent;
  private final NonBlockingHashMapLong<PostOrderCallback> postOrderCallbacks;
  private final NonBlockingHashMapLong<CancelAllCallback> cancelAllCallbacks;

  private final BufferClaim claim = new BufferClaim();

  public ExchangeClient(Publication publication, EventHandler handler, Subscription subscription) {
    this.publication = publication;
    this.postOrderCallbacks = new NonBlockingHashMapLong<>();
    this.cancelAllCallbacks = new NonBlockingHashMapLong<>();
    this.agent = new ReceiveAgent(postOrderCallbacks, cancelAllCallbacks, handler, subscription);
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

  private static BaseCallback.ErrorType interpretError(long claimResult) {
    return switch (claimResult) {
      case Publication.ADMIN_ACTION, Publication.BACK_PRESSURED ->
          BaseCallback.ErrorType.BACKPRESSURE;

      case Publication.CLOSED, Publication.MAX_POSITION_EXCEEDED ->
          BaseCallback.ErrorType.CORRUPT_SESSION;

      case Publication.NOT_CONNECTED -> BaseCallback.ErrorType.NOT_CONNECTED;

      default -> throw illegal("Unrecognized error code " + claimResult);
    };
  }

  private static final class ReceiveAgent extends ClassAgent {
    private final MessageHeader header = new MessageHeader();
    private final PostOrderResponse postOrderResponse = new PostOrderResponse();
    private final CancelAllResponse cancelAllResponse = new CancelAllResponse();

    // FIXME this is perhaps not the best data structure for this use-case.
    //  removes leave behind tombstones, which are not re-used since keys do not repeat,
    //  which forces a somewhat expensive periodic compaction.
    //  Consider replacing this with a purpose-built data structure instead.
    private final NonBlockingHashMapLong<PostOrderCallback> postOrderCallbacks;

    private final NonBlockingHashMapLong<CancelAllCallback> cancelAllCallbacks;

    private final EventHandler handler;
    private final Subscription subscription;

    private ReceiveAgent(
        NonBlockingHashMapLong<PostOrderCallback> postOrderCallbacks,
        NonBlockingHashMapLong<CancelAllCallback> cancelAllCallbacks,
        EventHandler handler,
        Subscription subscription) {
      this.postOrderCallbacks = postOrderCallbacks;
      this.cancelAllCallbacks = cancelAllCallbacks;
      this.handler = handler;
      this.subscription = subscription;
    }

    @Override
    public int doWork() throws Exception {
      return 0;
    }

    private boolean dispatch(DirectBuffer buffer, int offset, int length) {
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
            handler.onDispatchError(illegal("Unknown message type " + messageType));
            return true;
          }
        }
      } catch (RuntimeException e) {
        handler.onDispatchError(e);
        return true;
      }
    }

    private static <T> T removeOrThrow(NonBlockingHashMapLong<T> map, long key) {
      T value = map.remove(key);
      if (value == null) throw illegal("Callback not registered for correlation id " + key);
      return value;
    }
  }
}
