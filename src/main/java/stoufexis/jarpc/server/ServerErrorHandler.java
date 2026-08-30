package stoufexis.jarpc.server;

import org.agrona.ErrorHandler;
import stoufexis.jarpc.model.ErrorCode;

public interface ServerErrorHandler extends ErrorHandler {
  void onInternalError(long clientId, long correlationId, int errorCode);

  void onProcessingError(long clientId, long correlationId, int messageType);

  void onCorruptPublication(ErrorCode code);
}
