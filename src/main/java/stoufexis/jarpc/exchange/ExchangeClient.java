package stoufexis.jarpc.exchange;

import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import stoufexis.jarpc.model.BaseCallback;
import stoufexis.jarpc.util.ClassAgent;
import stoufexis.jarpc.util.EventHandler;
import stoufexis.jarpc.util.MessageHeader;

public class ExchangeClient implements Exchange {

  private final ReceiveAgent agent;

  public ExchangeClient(EventHandler handler) {
    this.agent = new ReceiveAgent(handler);
  }

  @Override
  public void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback) {
  }

  @Override
  public void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback) {
  }

  private static final class ReceiveAgent extends ClassAgent {
    private final MessageHeader header = new MessageHeader();
    private final PostOrderResponse postOrderResponse = new PostOrderResponse();
    private final CancelAllResponse cancelAllResponse = new CancelAllResponse();

    private final Long2ObjectHashMap<PostOrderCallback> postOrderCallbacks =
        new Long2ObjectHashMap<>();

    private final Long2ObjectHashMap<CancelAllCallback> cancelAllCallbacks =
        new Long2ObjectHashMap<>();

    private final EventHandler handler;

    private ReceiveAgent(EventHandler handler) {
      this.handler = handler;
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

    private static <T> T removeOrThrow(Long2ObjectHashMap<T> map, long key) {
      T value = map.remove(key);
      if (value == null) throw illegal("Callback not registered for correlation id " + key);
      return value;
    }

    private static IllegalStateException illegal(String message) {
      return new IllegalStateException(message);
    }
  }
}
