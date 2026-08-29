package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.model.Encode;
import stoufexis.jarpc.model.Poll;

public interface SingleThreadedExchangeServer extends Poll {

  PostOrderResponseEncode claimPostOrder();

  CancelAllResponseEncode claimCancelAll();

  interface PostOrderRequestHandler {
    boolean onRequest(long correlationId, PostOrderRequestDecode t);
  }

  interface CancelAllRequestHandler {
    boolean onRequest(long correlationId, CancelAllRequestDecode t);
  }

  interface PostOrderResponseEncode extends Encode {
    void setStatusCode(int statusCode);
  }

  interface CancelAllResponseEncode extends Encode {
    void setStatusCode(int statusCode);
  }

  interface CancelAllRequestDecode {}

  interface PostOrderRequestDecode {
    int getBaseAssetId();

    int getQuoteAssetId();

    long getQuantityUnscaled();

    int getQuantityScale();

    long getRateUnscaled();

    int getRateScale();
  }
}
