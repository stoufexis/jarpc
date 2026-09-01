package stoufexis.jarpc.lib.client;

import org.agrona.ErrorHandler;
import stoufexis.jarpc.lib.model.ErrorCode;
import stoufexis.jarpc.lib.util.OnCorruptPublication;

public interface ClientErrorHandler extends ErrorHandler, OnCorruptPublication {
  void onCallbackNotFound(long correlationId, String type);

  void onCorruptPublication(ErrorCode code);
}
