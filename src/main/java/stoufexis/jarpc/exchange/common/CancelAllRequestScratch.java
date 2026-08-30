package stoufexis.jarpc.exchange.common;

import stoufexis.jarpc.exchange.client.ConcurrentExchangeClient;

public final class CancelAllRequestScratch
    implements CancelAllRequestEncode, CancelAllRequestDecode {

  private ConcurrentExchangeClient.CancelAllResponseHandler handler;

  public static void copy(CancelAllRequestScratch scratch1, CancelAllRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      CancelAllRequestScratch scratch,
      CancelAllRequestDecode decode,
      ConcurrentExchangeClient.CancelAllResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public ConcurrentExchangeClient.CancelAllResponseHandler getHandler() {
    return handler;
  }

  public void setHandler(ConcurrentExchangeClient.CancelAllResponseHandler handler) {
    this.handler = handler;
  }
}
