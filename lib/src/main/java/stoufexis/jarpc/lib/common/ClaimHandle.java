package stoufexis.jarpc.lib.common;

import static stoufexis.jarpc.lib.common.Util.illegal;

import io.aeron.logbuffer.BufferClaim;

public final class ClaimHandle {
  private final BufferClaim claim = new BufferClaim();

  private ErrorCode code;
  private long correlationId;

  public boolean isFailed() {
    return this.code != null;
  }

  public void setSuccess(long correlationId) {
    this.correlationId = correlationId;
    this.code = null;
  }

  public void setFailed(ErrorCode errorCode) {
    this.correlationId = 0;
    this.code = errorCode;
  }

  public BufferClaim getClaim() {
    return claim;
  }

  public ErrorCode getCode() {
    return code;
  }

  public long getCorrelationId() {
    checkFailed();
    return this.correlationId;
  }

  public void commit() {
    checkFailed();
    claim.commit();
  }

  public void abort() {
    checkFailed();
    claim.abort();
  }

  private void checkFailed() {
    if (code != null) throw illegal("claim failed");
  }
}
