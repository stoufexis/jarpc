package stoufexis.jarpc.lib.common;

import io.aeron.*;
import stoufexis.jarpc.lib.server.Images;

public final class Util {
  private Util() {}

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

  public static Subscription createClientSubscription(Aeron aeron, ConnectionConfig cfg) {
    return aeron.addSubscription(
        new ChannelUriStringBuilder()
            .media(cfg.mediaString())
            .controlMode("response")
            .controlEndpoint(cfg.responseControl())
            .build(),
        cfg.responseStreamId());
  }

  public static Publication createExclusiveClientPublication(
      Aeron aeron, Subscription subscription, ConnectionConfig cfg) {
    return aeron.addExclusivePublication(
        new ChannelUriStringBuilder()
            .media(cfg.mediaString())
            .endpoint(cfg.requestEndpoint())
            .responseCorrelationId(subscription.registrationId())
            .build(),
        cfg.requestStreamId());
  }

  public static Subscription createServerSubscription(
      Aeron aeron, Images images, ConnectionConfig cfg) {
    return aeron.addSubscription(
        new ChannelUriStringBuilder()
            .media(cfg.mediaString())
            .endpoint(cfg.requestEndpoint())
            .build(),
        cfg.requestStreamId(),
        images::enqueueAvailableImage,
        images::enqueueUnavailableImage);
  }

  public static Publication createExclusiveServerPublication(
      Aeron aeron, long clientId, ConnectionConfig cfg) {
    return aeron.addExclusivePublication(
        new ChannelUriStringBuilder()
            .media(cfg.mediaString())
            .controlMode("response")
            .controlEndpoint(cfg.responseControl())
            .responseCorrelationId(clientId)
            .build(),
        cfg.responseStreamId());
  }
}
