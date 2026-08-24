package stoufexis.jarpc.exchange;

public interface Exchange {
  void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback);

  void cancelAllOrders(long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback {
    boolean onResponse(long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback {
    boolean onResponse(long correlationId, CancelAllResponse t);
  }
}
