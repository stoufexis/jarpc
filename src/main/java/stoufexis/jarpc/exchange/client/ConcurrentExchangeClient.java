package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.lib.client.ClientHandler;
import stoufexis.jarpc.exchange.common.CancelAllRequestDecode;
import stoufexis.jarpc.exchange.common.CancelAllResponseDecode;
import stoufexis.jarpc.exchange.common.PostOrderRequestDecode;
import stoufexis.jarpc.exchange.common.PostOrderResponseDecode;

public interface ConcurrentExchangeClient {
  boolean postOrder(PostOrderRequestDecode request, PostOrderResponseHandler response);

  boolean cancelAll(CancelAllRequestDecode request, CancelAllResponseHandler response);

  interface PostOrderResponseHandler extends ClientHandler {
    boolean onResponse(PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler extends ClientHandler {
    boolean onResponse(CancelAllResponseDecode t);
  }
}
