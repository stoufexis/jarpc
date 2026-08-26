package stoufexis.jarpc.util;

import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;

import java.util.Objects;

public final class ResponseServer implements AutoCloseable {

  private static final int FRAGMENT_LIMIT = 10;

  private final Aeron aeron;
  private final OneToOneConcurrentArrayQueue<Image> availableImages;
  private final OneToOneConcurrentArrayQueue<Image> unavailableImages;
  private final JarpcServer handler;

  private final Subscription serverSubscription;
  private final ChannelUriStringBuilder responseUriBuilder;
  private final int responseStreamId;

  private final Agent agent = new ServerReceiveAgent();

  ResponseServer(
      Aeron aeron,
      OneToOneConcurrentArrayQueue<Image> availableImages,
      OneToOneConcurrentArrayQueue<Image> unavailableImages,
      JarpcServer handler,
      Subscription serverSubscription,
      ChannelUriStringBuilder responseUriBuilder,
      int responseStreamId) {
    this.aeron = aeron;
    this.availableImages = availableImages;
    this.unavailableImages = unavailableImages;
    this.handler = handler;
    this.serverSubscription = serverSubscription;
    this.responseUriBuilder = responseUriBuilder;
    this.responseStreamId = responseStreamId;
  }

  public static ResponseServer create(
      Aeron aeron,
      JarpcServer handler,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId) {
    Objects.requireNonNull(requestEndpoint, "subscriptionEndpoint must not be null");
    Objects.requireNonNull(responseControl, "responseEndpoint must not be null");

    ChannelUriStringBuilder requestUriBuilder =
        new ChannelUriStringBuilder()
            .media("udp")
            .endpoint(requestEndpoint)
            .responseEndpoint(responseControl);

    ChannelUriStringBuilder responseUriBuilder =
        new ChannelUriStringBuilder()
            .media("udp")
            .controlMode("response")
            .controlEndpoint(responseControl);

    OneToOneConcurrentArrayQueue<Image> availableImg = new OneToOneConcurrentArrayQueue<>(1024);
    OneToOneConcurrentArrayQueue<Image> unavailableImg = new OneToOneConcurrentArrayQueue<>(1024);

    Subscription serverSubscription =
        aeron.addSubscription(
            requestUriBuilder.build(),
            requestStreamId,
            image -> enqueueAvailableImage(availableImg, image),
            image -> enqueueUnavailableImage(unavailableImg, image));

    return new ResponseServer(
        aeron,
        availableImg,
        unavailableImg,
        handler,
        serverSubscription,
        responseUriBuilder,
        responseStreamId);
  }

  public Agent getAgent() {
    return agent;
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    CloseHelper.quietClose(serverSubscription);
    handler.closePublications();
  }

  private class ServerReceiveAgent extends ClassAgent implements ControlledFragmentHandler {
    private final ControlledFragmentAssembler requestAssembler =
        new ControlledFragmentAssembler(this);

    /**
     * Poll the server process messages and state.
     *
     * @return amount of work done.
     */
    public int doWork() {
      int work = 0;

      Image image;
      while (null != (image = availableImages.poll())) {
        work++;
        ensurePublicationExists(image);
      }

      while (null != (image = unavailableImages.poll())) {
        work++;
        removeSession(image);
      }

      return work + serverSubscription.controlledPoll(requestAssembler, FRAGMENT_LIMIT);
    }

    @Override
    public ControlledFragmentHandler.Action onFragment(
        DirectBuffer buffer, int offset, int length, Header header) {
      Image image = (Image) header.context();
      ensurePublicationExists(image);

      return handler.onMessage(image.correlationId(), buffer, offset, length, header)
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;
    }

    private void ensurePublicationExists(Image image) {
      // We dont need computeIfAbsent, put/remove only happen in the agent thread.
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

  // FIXME need to figure out a more graceful error path
  private static void enqueueAvailableImage(
      OneToOneConcurrentArrayQueue<Image> availableImages, Image image) {
    if (!availableImages.offer(image)) {
      throw new RuntimeException("Unable to enqueue new image");
    }
  }

  private static void enqueueUnavailableImage(
      OneToOneConcurrentArrayQueue<Image> unavailableImages, Image image) {
    if (!unavailableImages.offer(image)) {
      throw new RuntimeException("Unable to enqueue removed image");
    }
  }
}
