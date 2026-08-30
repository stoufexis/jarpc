package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.common.CancelAllResponseEncode;
import stoufexis.jarpc.exchange.common.PostOrderResponseEncode;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.Poll;

public interface SingleThreadedExchangeServer extends Poll {
  PostOrderResponseEncode claimPostOrder(
      long clientId, long correlationId, ClaimHandle claimHandle);

  CancelAllResponseEncode claimCancelAll(
      long clientId, long correlationId, ClaimHandle claimHandle);
}
