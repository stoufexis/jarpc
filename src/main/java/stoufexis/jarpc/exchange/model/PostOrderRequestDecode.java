package stoufexis.jarpc.exchange.model;

public interface PostOrderRequestDecode {
  int getBaseAssetId();

  int getQuoteAssetId();

  long getQuantityUnscaled();

  int getQuantityScale();

  long getRateUnscaled();

  int getRateScale();
}
