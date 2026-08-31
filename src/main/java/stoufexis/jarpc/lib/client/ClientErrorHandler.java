package stoufexis.jarpc.lib.client;

import org.agrona.ErrorHandler;
import stoufexis.jarpc.lib.model.ErrorCode;

public interface ClientErrorHandler extends ErrorHandler {
  void onCallbackNotFound(long correlationId, String type);

  void onCorruptPublication(ErrorCode code);
}
