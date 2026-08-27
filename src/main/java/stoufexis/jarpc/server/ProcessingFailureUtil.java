package stoufexis.jarpc.server;

import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import stoufexis.jarpc.model.ProcessingFailureResponse;
import stoufexis.jarpc.model.ErrorCode;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public class ProcessingFailureUtil {

  protected final ServerErrorHandler errorHandler;

  private final ProcessingFailureResponse processingFailureResponse =
      new ProcessingFailureResponse();

  public ProcessingFailureUtil(ServerErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public boolean sendProcessingFailure(Publication publication, long clientId, long correlationId, int baseMessageType) {
    processingFailureResponse.set(baseMessageType);

    BufferClaim claim = processingFailureResponse.getClaim();
    long result = publication.tryClaim(processingFailureResponse.getMessageSize(), claim);

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
      processingFailureResponse.encode(claim.buffer(), claim.offset());
      claim.commit();
      return true;
    } catch (RuntimeException e) {
      claim.abort();
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.ENCODE_ERROR);
      return true;
    }
  }
}
