package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.common.CancelAllRequestDecode;
import stoufexis.jarpc.exchange.common.CancelAllResponseEncode;
import stoufexis.jarpc.exchange.common.PostOrderRequestDecode;
import stoufexis.jarpc.exchange.common.PostOrderResponseEncode;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.Poll;

public interface SingleThreadedExchangeServer extends Poll {
  PostOrderResponseEncode claimPostOrder(
      long clientId, long correlationId, ClaimHandle claimHandle);

  CancelAllResponseEncode claimCancelAll(
      long clientId, long correlationId, ClaimHandle claimHandle);

  interface PostOrderRequestHandler {
    boolean onRequest(long clientId, long correlationId, PostOrderRequestDecode t);
  }

  interface CancelAllRequestHandler {
    boolean onRequest(long clientId, long correlationId, CancelAllRequestDecode t);
  }
}
