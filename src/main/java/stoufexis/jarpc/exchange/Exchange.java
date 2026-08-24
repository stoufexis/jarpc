package stoufexis.jarpc.exchange;

import stoufexis.jarpc.model.BaseCallback;

public interface Exchange {
  void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback);

  void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback extends BaseCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     *     re-tried
     */
    boolean onResponse(long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback extends BaseCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     *     re-tried
     */
    boolean onResponse(long correlationId, CancelAllResponse t);
  }
}
