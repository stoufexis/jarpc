package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientCallback;
import stoufexis.jarpc.exchange.model.*;

/**
 * Encode objects must be read in-place and not stored. They must be released before calling claim
 * again.
 */
public interface ExchangeClientLow {

  PostOrderRequestEncode claimPostOrder();

  CancelAllRequestEncode claimCancelAll();

  int poll(int limit);

  interface PostOrderResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, CancelAllResponseDecode t);
  }
}
