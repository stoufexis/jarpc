package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientHandler;
import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.Poll;

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
