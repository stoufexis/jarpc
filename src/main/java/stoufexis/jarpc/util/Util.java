package stoufexis.jarpc.util;

import io.aeron.Publication;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.ErrorCode;

public final class Util {
  private Util() {}

  public static <T> T removeCallbackOrThrow(NonBlockingHashMapLong<T> map, long key) {
    T value = map.remove(key);
    if (value == null) throw illegal("Callback not registered for correlation id " + key);
    return value;
  }

  public static IllegalStateException illegal(String message) {
    return new IllegalStateException(message);
  }

  public static ErrorCode interpretErrorCode(long claimResult) {
    return switch (claimResult) {
      case Publication.ADMIN_ACTION, Publication.BACK_PRESSURED -> ErrorCode.BACKPRESSURE;

      case Publication.CLOSED, Publication.MAX_POSITION_EXCEEDED -> ErrorCode.CORRUPT_SESSION;

      case Publication.NOT_CONNECTED -> ErrorCode.NOT_CONNECTED;

      default -> throw illegal("Unrecognized error code " + claimResult);
    };
  }
}
