package stoufexis.jarpc.error;

public class ClientDecodeError extends RuntimeException {
  public ClientDecodeError() {
    super("Client failed to process a response");
  }
}
