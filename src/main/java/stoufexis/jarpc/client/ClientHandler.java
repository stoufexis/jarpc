package stoufexis.jarpc.client;

public interface ClientHandler {
  boolean onClientDecodeError(long correlationId);

  boolean onServerDecodeError(long correlationId);
}
