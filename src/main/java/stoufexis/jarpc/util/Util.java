package stoufexis.jarpc.util;

import io.aeron.Publication;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.ClientCallback;

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

  public static void interpretError(long claimResult, long correlationId, ClientCallback callback) {
    switch (claimResult) {
      case Publication.ADMIN_ACTION, Publication.BACK_PRESSURED ->
          callback.onBackpressure(correlationId);

      case Publication.CLOSED, Publication.MAX_POSITION_EXCEEDED ->
          callback.onCorruptSession(correlationId);

      case Publication.NOT_CONNECTED -> callback.onNotConnected(correlationId);

      default -> throw illegal("Unrecognized error code " + claimResult);
    }
  }
}
