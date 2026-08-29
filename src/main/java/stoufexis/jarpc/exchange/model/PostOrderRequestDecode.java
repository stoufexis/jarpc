package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.model.Encode;

public interface PostOrderRequestDecode {
  int getBaseAssetId();

  int getQuoteAssetId();

  long getQuantityUnscaled();

  int getQuantityScale();

  long getRateUnscaled();

  int getRateScale();
}
