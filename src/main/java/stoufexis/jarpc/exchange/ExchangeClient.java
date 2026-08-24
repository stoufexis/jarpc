package stoufexis.jarpc.exchange;

import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import stoufexis.jarpc.util.EventHandler;

public class ExchangeClient implements Exchange {
  private final EventHandler handler;

  public ExchangeClient(EventHandler handler) {
    this.handler = handler;
  }

  @Override
  public void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback) {}

  @Override
  public void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback) {}

  private static final class ReceiveAgent {
    private final Int2ObjectHashMap<PostOrderCallback> postOrderCallbacks =
        new Int2ObjectHashMap<>();

    private final Int2ObjectHashMap<CancelAllCallback> cancelAllCallbacks =
        new Int2ObjectHashMap<>();

    private void dispatch(long reservedValue, DirectBuffer buffer, int offset, int length) {
      switch ((int) reservedValue) {
        case Catalog.postOrderId -> {}
        case Catalog.cancelAllOrdersId -> {}
      }
    }
  }
}
