package stoufexis.jarpc.util;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.server.Images;

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

  public static Subscription createClientSubscription(
      Aeron aeron, String responseControl, int responseStreamId) {
    ChannelUriStringBuilder responseUriBuilder =
        new ChannelUriStringBuilder()
            .media("udp")
            .controlMode("response")
            .controlEndpoint(responseControl);

    return aeron.addSubscription(responseUriBuilder.build(), responseStreamId);
  }

  public static Publication createClientPublication(
      Aeron aeron, String requestEndpoint, int requestStreamId, Subscription subscription) {
    ChannelUriStringBuilder requestUriBuilder =
        new ChannelUriStringBuilder().media("udp").endpoint(requestEndpoint);

    return aeron.addPublication(
        requestUriBuilder.responseCorrelationId(subscription.registrationId()).build(),
        requestStreamId);
  }

  public static Subscription createServerSubscription(
      Aeron aeron,
      Images images,
      String requestEndpoint,
      String responseControl,
      int requestStreamId) {
    return aeron.addSubscription(
        new ChannelUriStringBuilder()
            .media("udp")
            .endpoint(requestEndpoint)
            .responseEndpoint(responseControl)
            .build(),
        requestStreamId,
        images::enqueueAvailableImage,
        images::enqueueUnavailableImage);
  }
}
