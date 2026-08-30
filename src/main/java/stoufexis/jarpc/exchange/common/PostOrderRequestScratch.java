package stoufexis.jarpc.exchange.common;

import stoufexis.jarpc.exchange.client.PostOrderResponseHandler;

public final class PostOrderRequestScratch
    implements PostOrderRequestDecode, PostOrderRequestEncode {
  private int baseAssetId;
  private int quoteAssetId;
  private long quantityUnscaled;
  private int quantityScale;
  private long rateUnscaled;
  private int rateScale;
  private PostOrderResponseHandler handler;

  public static void copy(PostOrderRequestScratch scratch1, PostOrderRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      PostOrderRequestScratch scratch,
      PostOrderRequestDecode decode,
      PostOrderResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public PostOrderResponseHandler getHandler() {
    return handler;
  }

  @Override
  public int getBaseAssetId() {
    return baseAssetId;
  }

  @Override
  public int getQuoteAssetId() {
    return quoteAssetId;
  }

  @Override
  public long getQuantityUnscaled() {
    return quantityUnscaled;
  }

  @Override
  public int getQuantityScale() {
    return quantityScale;
  }

  @Override
  public long getRateUnscaled() {
    return rateUnscaled;
  }

  @Override
  public int getRateScale() {
    return rateScale;
  }

  @Override
  public void setBaseAssetId(int baseAssetId) {
    this.baseAssetId = baseAssetId;
  }

  @Override
  public void setQuoteAssetId(int quoteAssetId) {
    this.quoteAssetId = quoteAssetId;
  }

  @Override
  public void setQuantityUnscaled(long quantityUnscaled) {
    this.quantityUnscaled = quantityUnscaled;
  }

  @Override
  public void setQuantityScale(int quantityScale) {
    this.quantityScale = quantityScale;
  }

  @Override
  public void setRateUnscaled(long rateUnscaled) {
    this.rateUnscaled = rateUnscaled;
  }

  @Override
  public void setRateScale(int rateScale) {
    this.rateScale = rateScale;
  }

  public void setHandler(PostOrderResponseHandler handler) {
    this.handler = handler;
  }
}
