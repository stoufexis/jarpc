package stoufexis.sample.generated.execution.common;

public final class PostLimitOrderResponseScratch implements PostLimitOrderResponseDecode, PostLimitOrderResponseEncode {

  private long generatedId;


  private int statusCode;




  private long correlationId;
  private long clientId;

  public static void copy(PostLimitOrderResponseScratch scratch1, PostLimitOrderResponseScratch scratch2) {
    setter(scratch1, scratch2, scratch2.clientId, scratch2.correlationId);
  }

  public static void setter(
      PostLimitOrderResponseScratch scratch,
      PostLimitOrderResponseDecode decode,
      long clientId,
      long correlationId) {
    scratch.set(decode);
    scratch.setClientId(clientId);
    scratch.setCorrelationId(correlationId);
  }


  @Override
  public long getGeneratedId() {
    return generatedId;
  }

  @Override
  public void setGeneratedId(long generatedId) {
    this.generatedId = generatedId;
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

