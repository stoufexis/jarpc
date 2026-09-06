package stoufexis.jarpc.lib.server;

/** Base interface for all server state machines, defining error handling hooks. */
public interface ServerStateMachine {
  void onClientDisconnected(long clientId);

  void onProcessingError(
      long clientId, long correlationId, int messageType, RuntimeException error);

  void onError(Throwable throwable);
}
