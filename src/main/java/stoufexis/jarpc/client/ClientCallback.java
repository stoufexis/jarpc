package stoufexis.jarpc.client;

public interface ClientCallback {
  boolean onClientDecodeError(int correlationId, RuntimeException exception);

  boolean onServerDecodeError(int correlationId);
}
