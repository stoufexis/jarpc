package stoufexis.jarpc.exchange.common;

public interface PostOrderRequestEncode {
  void setBaseAssetId(int baseAssetId);

  void setQuoteAssetId(int quoteAssetId);

  void setQuantityUnscaled(long quantityUnscaled);

  void setQuantityScale(int quantityScale);

  void setRateUnscaled(long rateUnscaled);

  void setRateScale(int rateScale);

  default void set(PostOrderRequestDecode decode) {
    setBaseAssetId(decode.getBaseAssetId());
    setQuoteAssetId(decode.getQuoteAssetId());
    setQuantityUnscaled(decode.getQuantityUnscaled());
    setQuantityScale(decode.getQuantityScale());
    setRateUnscaled(decode.getRateUnscaled());
    setRateScale(decode.getRateScale());
  }
}
