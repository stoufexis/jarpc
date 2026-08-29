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
    throw illegal("claim failed");
  }

  @Override
  public final void commit() {
    throw illegal("claim failed");
  }

  @Override
  public final void abort() {
    throw illegal("claim failed");
  }
}
