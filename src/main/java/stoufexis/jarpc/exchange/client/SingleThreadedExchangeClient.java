package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientCallback;
import stoufexis.jarpc.model.Encode;
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

  interface PostOrderResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, PostOrderResponseDecode t);
  }

  interface CancelAllResponseHandler extends ClientCallback {
    boolean onResponse(long correlationId, CancelAllResponseDecode t);
  }

  interface PostOrderRequestEncode extends Encode {
    void setBaseAssetId(int baseAssetId);

    void setQuoteAssetId(int quoteAssetId);

    void setQuantityUnscaled(long quantityUnscaled);

    void setQuantityScale(int quantityScale);

    void setRateUnscaled(long rateUnscaled);

    void setRateScale(int rateScale);
  }

  interface CancelAllRequestEncode extends Encode {}

  interface PostOrderResponseDecode {
    int getStatusCode();
  }

  interface CancelAllResponseDecode {
    int getStatusCode();
  }
}
