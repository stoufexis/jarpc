package stoufexis.jarpc.util;

public final class Util {
  private Util() {
  }

  public static IllegalStateException illegal(String message) {
    return new IllegalStateException(message);
  }
}
