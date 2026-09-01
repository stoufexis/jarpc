package stoufexis.sample.generated.execution.common;

public interface PostLimitOrderRequestEncode {

  void setAssetId(int assetId);


  void setQuantityUnscaled(long quantityUnscaled);


  void setRateUnscaled(long rateUnscaled);


  void setQuantityScale(int quantityScale);


  void setRateScale(int rateScale);




  default void set(PostLimitOrderRequestDecode decode) {

    setAssetId(decode.getAssetId());

    setQuantityUnscaled(decode.getQuantityUnscaled());

    setRateUnscaled(decode.getRateUnscaled());

    setQuantityScale(decode.getQuantityScale());

    setRateScale(decode.getRateScale());


  }
}

