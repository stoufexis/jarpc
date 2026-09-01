package stoufexis.sample.generated.execution.common;

import stoufexis.sample.generated.execution.client.ExecutionConcurrentClient;

public final class PostLimitOrderRequestScratch implements PostLimitOrderRequestDecode, PostLimitOrderRequestEncode {

  private int assetId;


  private long quantityUnscaled;


  private long rateUnscaled;


  private int quantityScale;


  private int rateScale;



  private ExecutionConcurrentClient.PostLimitOrderResponseHandler handler;

  public static void copy(PostLimitOrderRequestScratch scratch1, PostLimitOrderRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      PostLimitOrderRequestScratch scratch,
      PostLimitOrderRequestDecode decode,
      ExecutionConcurrentClient.PostLimitOrderResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public ExecutionConcurrentClient.PostLimitOrderResponseHandler getHandler() {
    return handler;
  }


  @Override
  public int getAssetId() {
    return assetId;
  }

  @Override
  public void setAssetId(int assetId) {
    this.assetId = assetId;
  }


  @Override
  public long getQuantityUnscaled() {
    return quantityUnscaled;
  }

  @Override
  public void setQuantityUnscaled(long quantityUnscaled) {
    this.quantityUnscaled = quantityUnscaled;
  }


  @Override
  public long getRateUnscaled() {
    return rateUnscaled;
  }

  @Override
  public void setRateUnscaled(long rateUnscaled) {
    this.rateUnscaled = rateUnscaled;
  }


  @Override
  public int getQuantityScale() {
    return quantityScale;
  }

  @Override
  public void setQuantityScale(int quantityScale) {
    this.quantityScale = quantityScale;
  }


  @Override
  public int getRateScale() {
    return rateScale;
  }

  @Override
  public void setRateScale(int rateScale) {
    this.rateScale = rateScale;
  }




  public void setHandler(ExecutionConcurrentClient.PostLimitOrderResponseHandler handler) {
    this.handler = handler;
  }
}

