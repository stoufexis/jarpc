package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.exchange.model.CancelAllRequestDecode;
import stoufexis.jarpc.exchange.model.PostOrderRequestDecode;

/**
 * Futures-based client. This produces relatively high GC pressure and high latency compared to the
 * single-threaded variant, but it is simple to use across many programming styles.
 */
public interface ConcurrentExchangeClient {
  boolean postOrder(PostOrderRequestDecode request, PostOrderResponseHandler response);

  boolean cancelAll(CancelAllRequestDecode request, CancelAllResponseHandler response);
}
