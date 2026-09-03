package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;
import stoufexis.sample.generated.client.LeaseConcurrentClient;

public final class QueryRequestScratch implements QueryRequestDecode, QueryRequestEncode {

  private long key = 0;



  private LeaseConcurrentClient.QueryResponseHandler handler;

  public static void copy(QueryRequestScratch scratch1, QueryRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      QueryRequestScratch scratch,
      QueryRequestDecode decode,
      LeaseConcurrentClient.QueryResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public LeaseConcurrentClient.QueryResponseHandler getHandler() {
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




  public void setHandler(LeaseConcurrentClient.QueryResponseHandler handler) {
    this.handler = handler;
  }
}

