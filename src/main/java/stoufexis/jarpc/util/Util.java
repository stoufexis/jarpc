package stoufexis.jarpc.util;

import io.aeron.Publication;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.BaseCallback;

public final class Util {
  private Util() {}

  public static <T> T removeOrThrow(NonBlockingHashMapLong<T> map, long key) {
    T value = map.remove(key);
    if (value == null) throw illegal("Callback not registered for correlation id " + key);
    return value;
  }

  public static IllegalStateException illegal(String message) {
    return new IllegalStateException(message);
  }

  public static BaseCallback.ErrorType interpretError(long claimResult) {
    return switch (claimResult) {
      case Publication.ADMIN_ACTION, Publication.BACK_PRESSURED ->
          BaseCallback.ErrorType.BACKPRESSURE;

      case Publication.CLOSED, Publication.MAX_POSITION_EXCEEDED ->
          BaseCallback.ErrorType.CORRUPT_SESSION;

      case Publication.NOT_CONNECTED -> BaseCallback.ErrorType.NOT_CONNECTED;

      default -> throw illegal("Unrecognized error code " + claimResult);
    };
  }
}
