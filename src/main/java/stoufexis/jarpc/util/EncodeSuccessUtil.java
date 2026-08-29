package stoufexis.jarpc.util;

import io.aeron.logbuffer.BufferClaim;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.Encode;
import stoufexis.jarpc.model.ErrorCode;

public abstract class EncodeSuccessUtil implements Encode {
  private long correlationId;
  private BufferClaim claim;
  protected int offset;
  protected MutableDirectBuffer buffer;

  public final void setSuccess(long correlationId, int offset, BufferClaim claim) {
    this.correlationId = correlationId;
    this.offset = offset;
    this.buffer = claim.buffer();
    this.claim = claim;
  }

  @Override
  public final ErrorCode code() {
    return null;
  }

  @Override
  public final long correlationId() {
    return this.correlationId;
  }

  @Override
  public final void commit() {
    claim.commit();
  }

  @Override
  public final void abort() {
    claim.abort();
  }
}
