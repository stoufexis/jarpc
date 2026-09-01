package stoufexis.sample.generated.execution.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import stoufexis.sample.generated.execution.common.*;

public interface ExecutionSingleThreadedClient extends Poll {


  PostLimitOrderRequestEncode claimPostLimitOrder(ClaimHandle claimHandle);

  interface PostLimitOrderResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, PostLimitOrderResponseDecode t);
  }

  CancelOrderRequestEncode claimCancelOrder(ClaimHandle claimHandle);

  interface CancelOrderResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, CancelOrderResponseDecode t);
  }


}

