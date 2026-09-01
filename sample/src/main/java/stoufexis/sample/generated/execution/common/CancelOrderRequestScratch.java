package stoufexis.sample.generated.execution.common;

import stoufexis.sample.generated.execution.client.ExecutionConcurrentClient;

public final class CancelOrderRequestScratch implements CancelOrderRequestDecode, CancelOrderRequestEncode {

  private long generatedId;



  private ExecutionConcurrentClient.CancelOrderResponseHandler handler;

  public static void copy(CancelOrderRequestScratch scratch1, CancelOrderRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      CancelOrderRequestScratch scratch,
      CancelOrderRequestDecode decode,
      ExecutionConcurrentClient.CancelOrderResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public ExecutionConcurrentClient.CancelOrderResponseHandler getHandler() {
    return handler;
  }


  @Override
  public long getGeneratedId() {
    return generatedId;
  }

  @Override
  public void setGeneratedId(long generatedId) {
    this.generatedId = generatedId;
  }




  public void setHandler(ExecutionConcurrentClient.CancelOrderResponseHandler handler) {
    this.handler = handler;
  }
}

