package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;
import stoufexis.sample.generated.lease.client.LeaseConcurrentClient;

public final class RefreshRequestScratch implements RefreshRequestDecode, RefreshRequestEncode {

  private long key = 0;



  private LeaseConcurrentClient.RefreshResponseHandler handler;

  public static void copy(RefreshRequestScratch scratch1, RefreshRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      RefreshRequestScratch scratch,
      RefreshRequestDecode decode,
      LeaseConcurrentClient.RefreshResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public LeaseConcurrentClient.RefreshResponseHandler getHandler() {
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




  public void setHandler(LeaseConcurrentClient.RefreshResponseHandler handler) {
    this.handler = handler;
  }
}

