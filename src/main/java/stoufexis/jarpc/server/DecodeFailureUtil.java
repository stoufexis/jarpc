package stoufexis.jarpc.server;

import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import stoufexis.jarpc.model.DecodeFailureResponse;
import stoufexis.jarpc.model.ErrorCode;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public class DecodeFailureUtil {

  protected final ServerErrorHandler errorHandler;

  private final DecodeFailureResponse decodeFailureResponse = new DecodeFailureResponse();

  private Publication publication;

  public DecodeFailureUtil(ServerErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public void setPublication(Publication publication) {
    this.publication = publication;
  }

  public boolean sendDecodeFailure(long clientId, long correlationId, int baseMessageType) {
    if (publication == null) {
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.CLIENT_NOT_EXISTS);
      return true;
    }

    decodeFailureResponse.set(baseMessageType);

    BufferClaim claim = decodeFailureResponse.getClaim();
    long result = publication.tryClaim(decodeFailureResponse.getMessageSize(), claim);

    // Sending a decode failure is best-effort.
    if (result < 0) {
      ErrorCode code = interpretErrorCode(result);

      if (code == ErrorCode.BACKPRESSURE) {
        return false;
      } else {
        errorHandler.onInternalError(clientId, correlationId, code);
        return true;
      }
    }

    try {
      decodeFailureResponse.encode(claim.buffer(), claim.offset());
      claim.commit();
      return true;
    } catch (RuntimeException e) {
      claim.abort();
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.ENCODE_ERROR);
      return true;
    }
  }
}
