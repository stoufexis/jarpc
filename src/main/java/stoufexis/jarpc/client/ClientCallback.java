package stoufexis.jarpc.client;

public interface ClientCallback {
  boolean onClientDecodeError(long correlationId);

  boolean onServerDecodeError(long correlationId);
}
