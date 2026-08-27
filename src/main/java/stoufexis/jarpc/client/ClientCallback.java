package stoufexis.jarpc.client;

public interface ClientCallback {
  void onClientDecodeError(int correlationId, RuntimeException exception);

  void onServerDecodeError(int correlationId);
}
