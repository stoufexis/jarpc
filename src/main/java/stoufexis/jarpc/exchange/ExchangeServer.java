package stoufexis.jarpc.exchange;

import stoufexis.jarpc.model.BaseCallback;

/**
 * Correlation ids must be unique per-request, they are used internally for identifying each request/response pair.
 */
public interface ExchangeServer {
  void postOrder(long clientId, long correlationId, PostOrderRequest request, PostOrderCallback callback);

  void cancelAll(long clientId, long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback extends BaseCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     * re-tried
     */
    boolean onResponse(long clientId, long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback extends BaseCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     * re-tried
     */
    boolean onResponse(long clientId, long correlationId, CancelAllResponse t);
  }
}
