package stoufexis.jarpc.lib.client;

public interface ClientHandler {
  boolean onClientDecodeError(long correlationId);
}
