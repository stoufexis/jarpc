package stoufexis.jarpc.client;

import org.agrona.ErrorHandler;
import stoufexis.jarpc.model.ErrorCode;

public interface ClientErrorHandler extends ErrorHandler {
  void onCallbackNotFound(long correlationId, String type);

  void onCorruptPublication(ErrorCode code);
}
