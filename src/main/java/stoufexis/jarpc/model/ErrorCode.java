package stoufexis.jarpc.model;

public final class ErrorCode {
  private ErrorCode() {}

  public static final int BACKPRESSURE = -1;
  public static final int NOT_CONNECTED = -2;
  public static final int CORRUPT_SESSION = -3;
  public static final int ENCODE_ERROR = -4;
  public static final int CLIENT_NOT_EXISTS = -5;
}
