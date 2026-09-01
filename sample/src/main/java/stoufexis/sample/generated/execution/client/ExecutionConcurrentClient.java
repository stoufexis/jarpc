package stoufexis.sample.generated.execution.client;

import stoufexis.jarpc.lib.client.*;

import stoufexis.sample.generated.execution.common.*;

public interface ExecutionConcurrentClient {

  boolean postLimitOrder(PostLimitOrderRequestDecode request, PostLimitOrderResponseHandler response);

  interface PostLimitOrderResponseHandler extends ClientHandler {
    boolean onResponse(PostLimitOrderResponseDecode t);
  }

  boolean cancelOrder(CancelOrderRequestDecode request, CancelOrderResponseHandler response);

  interface CancelOrderResponseHandler extends ClientHandler {
    boolean onResponse(CancelOrderResponseDecode t);
  }


}

