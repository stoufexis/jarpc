package stoufexis.jarpc.server;

import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import stoufexis.jarpc.model.BaseCatalog;
import stoufexis.jarpc.model.MessageHeader;
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

  public boolean sendProcessingFailure(
      Publication publication,
      long clientId,
      long correlationId,
      boolean last,
      int baseMessageType) {
    processingFailureResponse.set(baseMessageType);

    MessageHeader header = processingFailureResponse.getHeader();
    BufferClaim claim = processingFailureResponse.getClaim();

    long result =
        publication.tryClaim(
            processingFailureResponse.getMessageSize() + MessageHeader.HEADER_SIZE, claim);

    // Sending a decode failure is best-effort.
    if (result < 0) {
      ErrorCode code = interpretErrorCode(result);

      // FIXME should retry on not connected too?
      if (code == ErrorCode.BACKPRESSURE /* || code == ErrorCode.NOT_CONNECTED*/) {
        return false;
      } else {
        errorHandler.onInternalError(clientId, correlationId, code);
        return true;
      }
    }

    try {
      header.set(correlationId, BaseCatalog.processingFailure, last);
      header.encode(claim.buffer(), claim.offset());
      processingFailureResponse.encode(claim.buffer(), claim.offset() + MessageHeader.HEADER_SIZE);
      claim.commit();
      return true;
    } catch (RuntimeException e) {
      claim.abort();
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.ENCODE_ERROR);
      return true;
    }
  }
}
