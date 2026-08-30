package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.Poll;

public interface SingleThreadedExchangeClient extends Poll {
  PostOrderRequestEncode claimPostOrder(ClaimHandle claimHandle);

  CancelAllRequestEncode claimCancelAll(ClaimHandle claimHandle);
}
