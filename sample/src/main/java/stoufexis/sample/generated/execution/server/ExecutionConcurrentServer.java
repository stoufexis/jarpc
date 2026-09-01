package stoufexis.sample.generated.execution.server;

import stoufexis.sample.generated.execution.common.*;
import stoufexis.jarpc.lib.model.ClientHook;

public interface ExecutionConcurrentServer extends ClientHook {


  void registerPostLimitOrder(PostLimitOrderResponseHandler postLimitOrderResponse);

  boolean postLimitOrder(long clientId, long correlationId, PostLimitOrderRequestDecode request);

  interface PostLimitOrderResponseHandler {
    boolean onResponse(long clientId, long correlationId, PostLimitOrderResponseDecode t);
  }

  void registerCancelOrder(CancelOrderResponseHandler cancelOrderResponse);

  boolean cancelOrder(long clientId, long correlationId, CancelOrderRequestDecode request);

  interface CancelOrderResponseHandler {
    boolean onResponse(long clientId, long correlationId, CancelOrderResponseDecode t);
  }


}

