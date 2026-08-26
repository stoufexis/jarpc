package stoufexis.jarpc.exchange;

/**
 * Correlation ids must be unique per-request, they are used internally for identifying each
 * request/response pair.
 */
public interface ExchangeServer {
  /**
   * request object must be completely used before the method returns, it must not be referenced in
   * anything that outlives the method.
   */
  boolean postOrder(
      long clientId, long correlationId, PostOrderRequest request, PostOrderCallback callback);

  /**
   * request object must be completely used before the method returns, it must not be referenced in
   * anything that outlives the method.
   */
  boolean cancelAll(
      long clientId, long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     *     re-tried
     */
    boolean onResponse(long clientId, long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * @param correlationId
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     *     re-tried
     */
    boolean onResponse(long clientId, long correlationId, CancelAllResponse t);
  }
}
