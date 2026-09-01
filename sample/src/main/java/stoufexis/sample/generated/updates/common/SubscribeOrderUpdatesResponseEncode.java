package stoufexis.sample.generated.updates.common;

public interface SubscribeOrderUpdatesResponseEncode {

  void setAssetId(int assetId);


  void setRequestedQuantityUnscaled(long requestedQuantityUnscaled);


  void setFilledQuantityUnscaled(long filledQuantityUnscaled);


  void setRateUnscaled(long rateUnscaled);


  void setQuantityScale(int quantityScale);


  void setRateScale(int rateScale);


  void setStatusCode(int statusCode);




  default void set(SubscribeOrderUpdatesResponseDecode decode) {

    setAssetId(decode.getAssetId());

    setRequestedQuantityUnscaled(decode.getRequestedQuantityUnscaled());

    setFilledQuantityUnscaled(decode.getFilledQuantityUnscaled());

    setRateUnscaled(decode.getRateUnscaled());

    setQuantityScale(decode.getQuantityScale());

    setRateScale(decode.getRateScale());

    setStatusCode(decode.getStatusCode());


  }
}

