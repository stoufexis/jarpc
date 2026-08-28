package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.exchange.model.CancelAllRequest;
import stoufexis.jarpc.exchange.model.CancelAllResponse;
import stoufexis.jarpc.exchange.model.PostOrderRequest;
import stoufexis.jarpc.exchange.model.PostOrderResponse;
import stoufexis.jarpc.client.ClientCallback;

/**
 * Correlation ids must be unique per-request, they are used internally for identifying each
 * request/response pair.
 */
public interface ExchangeClient {
  int postOrder(PostOrderRequest request, PostOrderCallback callback);

  int cancelAll(CancelAllRequest request, CancelAllCallback callback);

  interface PostOrderCallback extends ClientCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * <p>Implementations should not throw.
     *
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     *     re-tried
     */
    boolean onResponse(boolean last, int correlationId, PostOrderResponse t);
  }

  interface CancelAllCallback extends ClientCallback {
    /**
     * Response object must be used and released by the time the method exits. Do not store or
     * re-use the object beyond this method's scope.
     *
     * <p>Implementations should not throw.
     *
     * @param t
     * @return true when the response was accepted, false when it was not and delivery must be
     *     re-tried
     */
    boolean onResponse(boolean last, int correlationId, CancelAllResponse t);
  }
}
