package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientCallback;
import stoufexis.jarpc.exchange.model.*;

public interface ExchangeClientLow {

  PostOrderRequestEncode claimPostOrder();

  CancelAllRequestEncode claimCancelAll();

  int poll(PostOrderResponseHandler postOrderHandler, CancelAllResponseHandler cancelAllCallback);

  interface PostOrderResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, CancelAllResponseDecode t);
  }
}
