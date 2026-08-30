package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.common.CancelAllRequestDecode;
import stoufexis.jarpc.exchange.common.CancelAllResponseDecode;
import stoufexis.jarpc.exchange.common.PostOrderRequestDecode;
import stoufexis.jarpc.exchange.common.PostOrderResponseDecode;

public interface ConcurrentExchangeServer {

  void registerHandlers(
      PostOrderResponseHandler postOrderResponse, CancelAllResponseHandler cancelAllResponse);

  boolean postOrder(long clientId, long correlationId, PostOrderRequestDecode request);

  boolean cancelAll(long clientId, long correlationId, CancelAllRequestDecode request);

  interface PostOrderResponseHandler {
    boolean onResponse(long clientId, long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler {
    boolean onResponse(long clientId, long correlationId, CancelAllResponseDecode t);
  }
}
