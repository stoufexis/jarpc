package stoufexis.jarpc.lib.server;

public interface ServerStateMachine {
  void onClientDisconnected(long clientId);

  void onProcessingError(
      long clientId, long correlationId, int messageType, RuntimeException error);

  void onError(Throwable throwable);
}
