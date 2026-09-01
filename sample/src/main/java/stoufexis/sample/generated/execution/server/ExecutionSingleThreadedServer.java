package stoufexis.sample.generated.execution.server;

import stoufexis.jarpc.lib.model.*;

import stoufexis.sample.generated.execution.common.*;

public interface ExecutionSingleThreadedServer extends Poll {


  PostLimitOrderResponseEncode claimPostLimitOrder(long clientId, long correlationId, ClaimHandle claimHandle);

  interface PostLimitOrderRequestHandler {
    boolean onRequest(long clientId, long correlationId, PostLimitOrderRequestDecode t);
  }

  CancelOrderResponseEncode claimCancelOrder(long clientId, long correlationId, ClaimHandle claimHandle);

  interface CancelOrderRequestHandler {
    boolean onRequest(long clientId, long correlationId, CancelOrderRequestDecode t);
  }


}

