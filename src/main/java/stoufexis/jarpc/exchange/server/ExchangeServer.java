package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.model.CancelAllRequest;
import stoufexis.jarpc.exchange.model.CancelAllResponse;
import stoufexis.jarpc.exchange.model.PostOrderRequest;
import stoufexis.jarpc.exchange.model.PostOrderResponse;
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
   * <p>Throwing an error results in a decode failure response to the client, which terminates a
   * response stream. Implementations should prefer sending application-level error messages instead
   * of throwing.
   */
  boolean postOrder(
      long clientId, long correlationId, PostOrderRequest request, PostOrderCallback callback);

  /**
   * request object must be completely used before the method returns, it must not be referenced in
   * anything that outlives the method.
   *
   * <p>Throwing an error results in a decode failure response to the client, which terminates a
   * response stream. Implementations should prefer sending application-level error messages instead
   * of throwing.
   */
  boolean cancelAll(
      long clientId, long correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback {

    ErrorCode onResponse(long clientId, long correlationId, boolean last, PostOrderResponse t);
  }

  interface CancelAllCallback {

    ErrorCode onResponse(long clientId, long correlationId, boolean last, CancelAllResponse t);
  }
}
