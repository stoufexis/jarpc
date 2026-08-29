package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientCallback;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.Poll;

/**
 * Encode objects must be read in-place and not stored. They must be released before calling claim
 * again.
 */
public interface SingleThreadedExchangeClient extends Poll {

  PostOrderRequestEncode claimPostOrder();

  CancelAllRequestEncode claimCancelAll();

  interface PostOrderResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, CancelAllResponseDecode t);
  }
}
