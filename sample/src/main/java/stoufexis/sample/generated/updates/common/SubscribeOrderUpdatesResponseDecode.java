package stoufexis.sample.generated.updates.common;

public interface SubscribeOrderUpdatesResponseDecode {

  int getAssetId();

  long getRequestedQuantityUnscaled();

  long getFilledQuantityUnscaled();

  long getRateUnscaled();

  int getQuantityScale();

  int getRateScale();

  int getStatusCode();


}

