package stoufexis.jarpc.util;

import io.aeron.*;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.server.Images;

public final class Util {
  private Util() {}

  public static <T> T getCallbackOrThrow(NonBlockingHashMapLong<T> map, long key, boolean last) {
    T value;
    if (last) {
      value = map.remove(key);
    } else {
      value = map.get(key);
    }
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

  public static Subscription createClientSubscription(
      Aeron aeron, String responseControl, int responseStreamId) {
    return aeron.addSubscription(
        new ChannelUriStringBuilder()
            .media("udp")
            .controlMode("response")
            .controlEndpoint(responseControl)
            .build(),
        responseStreamId);
  }

  public static Publication createClientPublication(
      Aeron aeron, String requestEndpoint, int requestStreamId, Subscription subscription) {
    return aeron.addPublication(
        new ChannelUriStringBuilder()
            .media("udp")
            .endpoint(requestEndpoint)
            .responseCorrelationId(subscription.registrationId())
            .build(),
        requestStreamId);
  }

  public static Subscription createServerSubscription(
      Aeron aeron, Images images, String requestEndpoint, int requestStreamId) {
    return aeron.addSubscription(
        new ChannelUriStringBuilder().media("udp").endpoint(requestEndpoint).build(),
        requestStreamId,
        images::enqueueAvailableImage,
        images::enqueueUnavailableImage);
  }

  public static Publication createServerPublication(
      Aeron aeron, Image image, String responseControl, int responseStreamId) {
    return aeron.addPublication(
        new ChannelUriStringBuilder()
            .media("udp")
            .controlMode("response")
            .controlEndpoint(responseControl)
            .responseCorrelationId(image.correlationId())
            .build(),
        responseStreamId);
  }
}
