package stoufexis.jarpc.lib.server;

import org.agrona.ErrorHandler;
import stoufexis.jarpc.lib.model.ErrorCode;

public interface ServerErrorHandler extends ErrorHandler {
  void onInternalError(long clientId, long correlationId, int errorCode);

  void onProcessingError(long clientId, long correlationId, int messageType);

  void onCorruptPublication(ErrorCode code);
}
