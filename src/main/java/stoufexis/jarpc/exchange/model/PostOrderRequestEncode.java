package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.model.Encode;

public interface PostOrderRequestEncode extends Encode {
  void setBaseAssetId(int baseAssetId);

  void setQuoteAssetId(int quoteAssetId);

  void setQuantityUnscaled(long quantityUnscaled);

  void setQuantityScale(int quantityScale);

  void setRateUnscaled(long rateUnscaled);

  void setRateScale(int rateScale);
}
