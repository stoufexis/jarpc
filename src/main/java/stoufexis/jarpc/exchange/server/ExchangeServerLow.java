package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.model.*;

public interface ExchangeServerLow {

  PostOrderResponseEncode claimPostOrder();

  CancelAllResponseEncode claimCancelAll();

  int poll(PostOrderRequestHandler postOrderHandler, CancelAllRequestHandler cancelAllHandler);

  interface PostOrderRequestHandler {
    boolean onRequest(long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllRequestHandler {
    boolean onRequest(long correlationId, CancelAllResponseDecode t);
  }
}
