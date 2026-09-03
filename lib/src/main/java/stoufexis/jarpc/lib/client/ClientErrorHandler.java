package stoufexis.jarpc.lib.client;

import stoufexis.jarpc.lib.common.ErrorCode;

public interface ClientErrorHandler {
  void onCallbackNotFound(long correlationId, String type);

  void onCorruptPublication(ErrorCode code);

  void onError(Throwable throwable);
}
