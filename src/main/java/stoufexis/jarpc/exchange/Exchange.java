package stoufexis.jarpc.exchange;

import stoufexis.jarpc.model.BaseCallback;

public interface Exchange {
  void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback);

  void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback extends BaseCallback {
    boolean onResponse(long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback extends BaseCallback {
    boolean onResponse(long correlationId, CancelAllResponse t);
  }
}
