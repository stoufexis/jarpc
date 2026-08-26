package stoufexis.jarpc.exchange;

import stoufexis.jarpc.model.ErrorCode;

/**
 * Correlation ids must be unique per-request, they are used internally for identifying each
 * request/response pair.
 */
public interface ExchangeServer {
  /**
   * request object must be completely used before the method returns, it must not be referenced in
   * anything that outlives the method.
   *
   * <p>Throwing an error results in no a decode failure response to the client
   */
  boolean postOrder(
      long clientId, long correlationId, PostOrderRequest request, PostOrderCallback callback);

  /**
   * request object must be completely used before the method returns, it must not be referenced in
   * anything that outlives the method.
   *
   * <p>Throwing an error results in no a decode failure response to the client
   */
  boolean cancelAll(
      long clientId, long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback {

    ErrorCode onResponse(long clientId, long correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback {

    ErrorCode onResponse(long clientId, long correlationId, CancelAllResponse t);
  }
}
