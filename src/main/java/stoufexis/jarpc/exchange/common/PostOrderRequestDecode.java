package stoufexis.jarpc.exchange.common;

public interface PostOrderRequestDecode {
  int getBaseAssetId();

  int getQuoteAssetId();

  long getQuantityUnscaled();

  int getQuantityScale();

  long getRateUnscaled();

  int getRateScale();
}
