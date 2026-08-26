package stoufexis.jarpc.exchange;

import stoufexis.jarpc.model.ClientCallback;

/**
 * Correlation ids must be unique per-request, they are used internally for identifying each request/response pair.
 */
public interface ExchangeClient {
  void postOrder(long correlationId, PostOrderRequest request, PostOrderCallback callback);

  void cancelAll(long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback extends ClientCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     * re-tried
     */
    boolean onResponse(long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback extends ClientCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     * re-tried
     */
    boolean onResponse(long correlationId, CancelAllResponse t);
  }
}
