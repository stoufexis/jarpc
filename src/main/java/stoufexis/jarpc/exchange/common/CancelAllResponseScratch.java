package stoufexis.jarpc.exchange.common;

public final class CancelAllResponseScratch
    implements CancelAllResponseDecode, CancelAllResponseEncode {
  private int statusCode;
  private long clientId;
  private long correlationId;

  public static void copy(CancelAllResponseScratch scratch1, CancelAllResponseScratch scratch2) {
    setter(scratch1, scratch2, scratch2.clientId, scratch2.correlationId);
  }

  public static void setter(
      CancelAllResponseScratch scratch,
      CancelAllResponseDecode decode,
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

  public long getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(long correlationId) {
    this.correlationId = correlationId;
  }

  public long getClientId() {
    return clientId;
  }

  public void setClientId(long clientId) {
    this.clientId = clientId;
  }
}
