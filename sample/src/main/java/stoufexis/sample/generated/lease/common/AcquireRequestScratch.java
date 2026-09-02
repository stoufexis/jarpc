package stoufexis.sample.generated.lease.common;

import stoufexis.sample.generated.lease.client.LeaseConcurrentClient;

public final class AcquireRequestScratch implements AcquireRequestDecode, AcquireRequestEncode {

  private long key = 0;


  private byte[] value = new byte[16];


  private int ttlSeconds = 0;



  private LeaseConcurrentClient.AcquireResponseHandler handler;

  public static void copy(AcquireRequestScratch scratch1, AcquireRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      AcquireRequestScratch scratch,
      AcquireRequestDecode decode,
      LeaseConcurrentClient.AcquireResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public LeaseConcurrentClient.AcquireResponseHandler getHandler() {
    return handler;
  }


  @Override
  public long getKey() {
    return key;
  }

  @Override
  public void setKey(long key) {
    this.key = key;
  }


  @Override
  public byte[] getValue() {
    return value;
  }

  @Override
  public void setValue(byte[] value) {
    System.arraycopy(value, 0, this.value, 0, 16);
  }


  @Override
  public int getTtlSeconds() {
    return ttlSeconds;
  }

  @Override
  public void setTtlSeconds(int ttlSeconds) {
    this.ttlSeconds = ttlSeconds;
  }




  public void setHandler(LeaseConcurrentClient.AcquireResponseHandler handler) {
    this.handler = handler;
  }
}

