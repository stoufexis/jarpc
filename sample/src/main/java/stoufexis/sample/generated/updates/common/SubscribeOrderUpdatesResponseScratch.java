package stoufexis.sample.generated.updates.common;

public final class SubscribeOrderUpdatesResponseScratch implements SubscribeOrderUpdatesResponseDecode, SubscribeOrderUpdatesResponseEncode {

  private int assetId;


  private long requestedQuantityUnscaled;


  private long filledQuantityUnscaled;


  private long rateUnscaled;


  private int quantityScale;


  private int rateScale;


  private int statusCode;




  private long correlationId;
  private long clientId;

  public static void copy(SubscribeOrderUpdatesResponseScratch scratch1, SubscribeOrderUpdatesResponseScratch scratch2) {
    setter(scratch1, scratch2, scratch2.clientId, scratch2.correlationId);
  }

  public static void setter(
      SubscribeOrderUpdatesResponseScratch scratch,
      SubscribeOrderUpdatesResponseDecode decode,
      long clientId,
      long correlationId) {
    scratch.set(decode);
    scratch.setClientId(clientId);
    scratch.setCorrelationId(correlationId);
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
  public long getRequestedQuantityUnscaled() {
    return requestedQuantityUnscaled;
  }

  @Override
  public void setRequestedQuantityUnscaled(long requestedQuantityUnscaled) {
    this.requestedQuantityUnscaled = requestedQuantityUnscaled;
  }

  @Override
  public long getFilledQuantityUnscaled() {
    return filledQuantityUnscaled;
  }

  @Override
  public void setFilledQuantityUnscaled(long filledQuantityUnscaled) {
    this.filledQuantityUnscaled = filledQuantityUnscaled;
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

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }



  public void setCorrelationId(long correlationId) {
    this.correlationId = correlationId;
  }

  public void setClientId(long clientId) {
    this.clientId = clientId;
  }

  public long getCorrelationId() {
    return correlationId;
  }

  public long getClientId() {
    return clientId;
  }
}

