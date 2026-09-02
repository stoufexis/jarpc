package stoufexis.jarpc.lib.server;

import stoufexis.jarpc.lib.util.OnCorruptPublication;

public interface ServerErrorHandler extends OnCorruptPublication {
  void onInternalError(long clientId, long correlationId, int errorCode);

  void onProcessingError(long clientId, long correlationId, int messageType);

  void onError(Throwable throwable);
}
