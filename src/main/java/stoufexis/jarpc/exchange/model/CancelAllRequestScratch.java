package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.exchange.client.CancelAllResponseHandler;
import stoufexis.jarpc.model.ErrorCode;

public final class CancelAllRequestScratch
    implements CancelAllRequestEncode, CancelAllRequestDecode {

  private CancelAllResponseHandler handler;

  public static void copy(CancelAllRequestScratch scratch1, CancelAllRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      CancelAllRequestScratch scratch,
      CancelAllRequestDecode decode,
      CancelAllResponseHandler handler) {
    scratch.setHandler(handler);
  }

  @Override
  public ErrorCode code() {
    return null;
  }

  @Override
  public long correlationId() {
    return 0;
  }

  @Override
  public void commit() {}

  @Override
  public void abort() {}

  public CancelAllResponseHandler getHandler() {
    return handler;
  }

  public void setHandler(CancelAllResponseHandler handler) {
    this.handler = handler;
  }
}
