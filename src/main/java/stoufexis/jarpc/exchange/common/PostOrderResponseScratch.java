package stoufexis.jarpc.exchange.common;

public final class PostOrderResponseScratch
    implements PostOrderResponseDecode, PostOrderResponseEncode {
  private int statusCode;
  private long clientId;
  private long correlationId;

  public static void copy(PostOrderResponseScratch scratch1, PostOrderResponseScratch scratch2) {
    setter(scratch1, scratch2, scratch2.clientId, scratch2.correlationId);
  }

  public static void setter(
      PostOrderResponseScratch scratch,
      PostOrderResponseDecode decode,
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
