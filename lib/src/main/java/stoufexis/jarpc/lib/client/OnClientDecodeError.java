package stoufexis.jarpc.lib.client;

public interface OnClientDecodeError {
  boolean onClientDecodeError(long correlationId);
}
