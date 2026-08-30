package stoufexis.jarpc.util;

import io.aeron.logbuffer.BufferClaim;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.Encode;
import stoufexis.jarpc.model.ErrorCode;

import static stoufexis.jarpc.util.Util.illegal;

public abstract class EncodeUtil implements Encode {
  private ErrorCode code;
  private long correlationId;
  private BufferClaim claim;
  protected int offset;
  protected MutableDirectBuffer buffer;

  public final void setSuccess(long correlationId, int offset, BufferClaim claim) {
    this.correlationId = correlationId;
    this.offset = offset;
    this.buffer = claim.buffer();
    this.claim = claim;
    this.code = null;
  }

  public final void setFailed(ErrorCode errorCode) {
    this.correlationId = 0;
    this.offset = 0;
    this.buffer = null;
    this.claim = null;
    this.code = errorCode;
  }

  @Override
  public final ErrorCode code() {
    return code;
  }

  @Override
  public final long correlationId() {
    checkFailed();
    return this.correlationId;
  }

  @Override
  public final void commit() {
    checkFailed();
    claim.commit();
  }

  @Override
  public final void abort() {
    checkFailed();
    claim.abort();
  }

  protected final void checkFailed() {
    if (code != null) throw illegal("claim failed");
  }
}
