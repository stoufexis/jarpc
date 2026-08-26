package stoufexis.jarpc.client;

public interface ClientCallback {
  void onClientDecodeError(long correlationId, RuntimeException exception);

  void onServerDecodeError(long correlationId);
}
