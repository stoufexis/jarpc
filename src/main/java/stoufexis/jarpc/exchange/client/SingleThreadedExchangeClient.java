package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.Poll;

/**
 * Low-latency, low-garbage client, meant to be used withing a single-threaded duty cycle.
 *
 * <p>Encode objects must be read in-place and not stored. They must be released before calling
 * claim again.
 */
public interface SingleThreadedExchangeClient extends Poll {
  PostOrderRequestEncode claimPostOrder();

  CancelAllRequestEncode claimCancelAll();
}
