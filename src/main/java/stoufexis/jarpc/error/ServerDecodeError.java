package stoufexis.jarpc.error;

public class ServerDecodeError extends RuntimeException {
  public ServerDecodeError() {
    super("Server failed to decode an error");
  }
}
