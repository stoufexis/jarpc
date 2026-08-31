package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.lib.client.ClientHandler;
import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.lib.model.ClaimHandle;
import stoufexis.jarpc.lib.model.Poll;

public interface SingleThreadedExchangeClient extends Poll {
  PostOrderRequestEncode claimPostOrder(ClaimHandle claimHandle);

  CancelAllRequestEncode claimCancelAll(ClaimHandle claimHandle);

  interface PostOrderResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, CancelAllResponseDecode t);
  }
}
