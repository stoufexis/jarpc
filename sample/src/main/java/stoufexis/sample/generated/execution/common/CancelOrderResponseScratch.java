package stoufexis.sample.generated.execution.common;

public final class CancelOrderResponseScratch implements CancelOrderResponseDecode, CancelOrderResponseEncode {

  private int statusCode;




  private long correlationId;
  private long clientId;

  public static void copy(CancelOrderResponseScratch scratch1, CancelOrderResponseScratch scratch2) {
    setter(scratch1, scratch2, scratch2.clientId, scratch2.correlationId);
  }

  public static void setter(
      CancelOrderResponseScratch scratch,
      CancelOrderResponseDecode decode,
      long clientId,
      long correlationId) {
    scratch.set(decode);
    scratch.setClientId(clientId);
    scratch.setCorrelationId(correlationId);
  }


  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }



  public void setCorrelationId(long correlationId) {
    this.correlationId = correlationId;
  }

  public void setClientId(long clientId) {
    this.clientId = clientId;
  }

  public long getCorrelationId() {
    return correlationId;
  }

  public long getClientId() {
    return clientId;
  }
}

