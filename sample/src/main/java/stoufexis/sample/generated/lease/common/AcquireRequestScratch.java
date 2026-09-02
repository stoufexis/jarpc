package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;
import stoufexis.sample.generated.lease.client.LeaseConcurrentClient;

public final class AcquireRequestScratch implements AcquireRequestDecode, AcquireRequestEncode {

  private long key = 0;


  private Bytes value = new Bytes(16);



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
  public long key() {
    return key;
  }

  @Override
  public void setKey(long key) {
    this.key = key;
  }


  @Override
  public Bytes value() {
    return value;
  }

  @Override
  public void setValue(Bytes value) {
    this.value.copy(value);
  }




  public void setHandler(LeaseConcurrentClient.AcquireResponseHandler handler) {
    this.handler = handler;
  }
}

