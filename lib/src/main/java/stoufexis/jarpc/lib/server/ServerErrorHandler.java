package stoufexis.jarpc.lib.server;

import org.agrona.ErrorHandler;
import stoufexis.jarpc.lib.model.ErrorCode;
import stoufexis.jarpc.lib.util.OnCorruptPublication;

public interface ServerErrorHandler extends ErrorHandler, OnCorruptPublication {
  void onInternalError(long clientId, long correlationId, int errorCode);

  void onProcessingError(long clientId, long correlationId, int messageType);
}
