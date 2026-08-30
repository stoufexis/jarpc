package stoufexis.jarpc.error;

import stoufexis.jarpc.model.ErrorCode;

public class PublicationError extends RuntimeException {
  public ErrorCode code;

  public PublicationError(ErrorCode code) {
    this.code = code;
    super(code.name());
  }
}
