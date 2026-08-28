package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.model.CancelAllRequest;
import stoufexis.jarpc.exchange.model.CancelAllResponse;
import stoufexis.jarpc.exchange.model.PostOrderRequest;
import stoufexis.jarpc.exchange.model.PostOrderResponse;

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
   *
   * <p>The correlationId is unique per (client id, request type) pair.
   */
  boolean postOrder(
      long clientId, int correlationId, PostOrderRequest request, PostOrderCallback callback);

  /**
   * request object must be completely used before the method returns, it must not be referenced in
   * anything that outlives the method.
   *
   * <p>Throwing an error results in a decode failure response to the client, which terminates a
   * response stream. Implementations should prefer sending application-level error messages instead
   * of throwing.
   *
   * <p>The correlationId is unique per (client id, request type) pair.
   */
  boolean cancelAll(
      long clientId, int correlationId, CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback {
    int onResponse(long clientId, int correlationId, boolean last, PostOrderResponse t);
  }

  interface CancelAllCallback {
    int onResponse(long clientId, int correlationId, boolean last, CancelAllResponse t);
  }
}
