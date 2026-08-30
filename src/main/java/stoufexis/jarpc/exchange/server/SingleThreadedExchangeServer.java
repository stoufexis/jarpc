package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.model.CancelAllResponseEncode;
import stoufexis.jarpc.exchange.model.PostOrderResponseEncode;
import stoufexis.jarpc.model.Poll;

public interface SingleThreadedExchangeServer extends Poll {
  PostOrderResponseEncode claimPostOrder();

  CancelAllResponseEncode claimCancelAll();
}
