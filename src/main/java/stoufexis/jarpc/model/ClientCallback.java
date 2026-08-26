package stoufexis.jarpc.model;

public interface ClientCallback {
  void onClientDecodeError(long correlationId, RuntimeException exception);

  void onServerDecodeError(long correlationId);
}
