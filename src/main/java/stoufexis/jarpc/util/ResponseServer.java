package stoufexis.jarpc.util;

import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;

import java.util.Objects;

public class ResponseServer implements AutoCloseable, Agent {

  private static final int FRAGMENT_LIMIT = 10;

  private final Aeron aeron;
  private final OneToOneConcurrentArrayQueue<Image> availableImages =
      new OneToOneConcurrentArrayQueue<>(1024);
  private final OneToOneConcurrentArrayQueue<Image> unavailableImages =
      new OneToOneConcurrentArrayQueue<>(1024);
  private final JarpcServer handler;
  private final int requestStreamId;
  private final int responseStreamId;
  private final ChannelUriStringBuilder requestUriBuilder;
  private final ChannelUriStringBuilder responseUriBuilder;
  private final ControlledFragmentAssembler requestAssembler =
      new ControlledFragmentAssembler(this::onControlledRequestMessage);

  private Subscription serverSubscription;

  public ResponseServer(
      Aeron aeron,
      JarpcServer handler,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId,
      String requestChannel,
      String responseChannel) {
    this.aeron = aeron;
    this.handler = handler;
    this.requestStreamId = requestStreamId;
    this.responseStreamId = responseStreamId;

    Objects.requireNonNull(requestEndpoint, "subscriptionEndpoint must not be null");
    Objects.requireNonNull(responseControl, "responseEndpoint must not be null");

    requestUriBuilder =
        null == requestChannel
            ? new ChannelUriStringBuilder()
            : new ChannelUriStringBuilder(requestChannel);
    requestUriBuilder.media("udp").endpoint(requestEndpoint).responseEndpoint(responseControl);
    responseUriBuilder =
        null == responseChannel
            ? new ChannelUriStringBuilder()
            : new ChannelUriStringBuilder(responseChannel);
    responseUriBuilder.media("udp").controlMode("response").controlEndpoint(responseControl);
  }

  /**
   * Poll the server process messages and state.
   *
   * @return amount of work done.
   */
  public int doWork() {
    int workCount = 0;

    if (null == serverSubscription) {
      serverSubscription =
          aeron.addSubscription(
              requestUriBuilder.build(),
              requestStreamId,
              this::enqueueAvailableImage,
              this::enqueueUnavailableImage);

      workCount++;
    }

    Image image;
    while (null != (image = availableImages.poll())) {
      workCount++;
      ensurePublicationExists(image);
    }

    while (null != (image = unavailableImages.poll())) {
      workCount++;
      removeSession(image);
    }

    workCount += serverSubscription.controlledPoll(requestAssembler, FRAGMENT_LIMIT);

    return workCount;
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    CloseHelper.quietClose(serverSubscription);
  }

  @Override
  public String roleName() {
    return "ResponseServer";
  }

  // FIXME need to figure out a more graceful error path
  private void enqueueAvailableImage(Image image) {
    if (!availableImages.offer(image)) {
      throw new RuntimeException("Unable to enqueue new image");
    }
  }

  private void enqueueUnavailableImage(Image image) {
    if (!unavailableImages.offer(image)) {
      throw new RuntimeException("Unable to enqueue removed image");
    }
  }

  private ControlledFragmentHandler.Action onControlledRequestMessage(
      DirectBuffer buffer, int offset, int length, Header header) {
    Image image = (Image) header.context();
    ensurePublicationExists(image);

    return handler.onMessage(image.correlationId(), buffer, offset, length, header)
        ? ControlledFragmentHandler.Action.CONTINUE
        : ControlledFragmentHandler.Action.ABORT;
  }

  private void ensurePublicationExists(Image image) {
    if (null == handler.getPublication(image.correlationId())) {
      Publication publication =
          aeron.addPublication(
              responseUriBuilder.responseCorrelationId(image.correlationId()).build(),
              responseStreamId);

      handler.putPublication(image.correlationId(), publication);
    }
  }

  private void removeSession(Image image) {
    requestAssembler.freeSessionBuffer(image.sessionId());
    CloseHelper.quietClose(handler.removePublication(image.correlationId()));
  }
}
