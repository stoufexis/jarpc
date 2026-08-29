package stoufexis.jarpc.client;

public interface ClientCallback {
  boolean onClientDecodeError(long correlationId, RuntimeException exception);

  boolean onServerDecodeError(long correlationId);
}
