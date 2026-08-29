package stoufexis.jarpc.util;

import stoufexis.jarpc.model.Encode;
import stoufexis.jarpc.model.ErrorCode;

import static stoufexis.jarpc.util.Util.illegal;

public abstract class EncodeFailUtil implements Encode {
  private ErrorCode code;

  public final void setErrorCode(ErrorCode errorCode) {
    this.code = errorCode;
  }

  @Override
  public final ErrorCode code() {
    return code;
  }

  @Override
  public final long correlationId() {
    fail();
    return 0; // unreachable, simply here to please the compiler
  }

  @Override
  public final void commit() {
    fail();
  }

  @Override
  public final void abort() {
    fail();
  }

  protected final void fail() {
    throw illegal("claim failed");
  }
}
