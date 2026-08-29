package stoufexis.jarpc.client;

import org.agrona.ErrorHandler;

public interface ClientErrorHandler extends ErrorHandler {
  void onCallbackNotFound(long correlationId, String type);
}
