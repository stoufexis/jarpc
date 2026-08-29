package stoufexis.jarpc.exchange.client;

import java.util.concurrent.CompletableFuture;

/**
 * Futures-based client. This produces relatively high GC pressure and high latency compared to the
 * single-threaded variant, but it is easier to use across different programming styles.
 */
public interface FuturesExchangeClient {

  boolean postOrder(PostOrderRequest request, CompletableFuture<PostOrderResponse> response);

  boolean cancelAll(CancelAllRequest request, CompletableFuture<CancelAllResponse> response);

  record PostOrderRequest(
      int baseAssetId,
      int quoteAssetId,
      long quantityUnscaled,
      int quantityScale,
      long rateUnscaled,
      int rateScale) {}

  record CancelAllRequest() {}

  record PostOrderResponse(int statusCode) {}

  record CancelAllResponse(int statusCode) {}
}
