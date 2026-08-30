package stoufexis.jarpc.server;

import org.agrona.ErrorHandler;

public interface ServerErrorHandler extends ErrorHandler {
  void onInternalError(long clientId, long correlationId, int errorCode);

  void onProcessingError(long clientId, long correlationId, int messageType);
}
