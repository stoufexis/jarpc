package stoufexis.jarpc.util;

import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;

import java.util.Objects;
import java.util.function.Function;

public class ResponseServer implements AutoCloseable, Agent {

  public abstract class ResponseHandler2 {
    private final Long2ObjectHashMap<Publication> clientToPublicationMap =
        new Long2ObjectHashMap<>();

    private void putPublication(long clientId, Publication pub) {
      clientToPublicationMap.put(clientId, pub);
    }

    private void removePublication(long clientId) {
      clientToPublicationMap.remove(clientId);
    }

    protected Publication getPublication(long clientId) {
      return clientToPublicationMap.get(clientId);
    }

    public abstract boolean onMessage(
        long clientId, DirectBuffer buffer, int offset, int length, Header header);
  }

  /** Interface to manage callback from the response server onto a session. */
  public interface ResponseHandler {
    /**
     * Called when a message is received via the request subscription.
     *
     * @param buffer containing the data.
     * @param offset at which the data begins.
     * @param length of the data in bytes.
     * @param header representing the metadata for the data.
     * @param responsePublication to send responses back to the client.
     * @return <code>true</code> if the message was processed otherwise.
     */
    boolean onMessage(
        DirectBuffer buffer,
        int offset,
        int length,
        Header header,
        Publication responsePublication);
  }

  private static final int FRAGMENT_LIMIT = 10;

  private final Aeron aeron;
  private final Long2ObjectHashMap<ResponseSession> clientToPublicationMap =
      new Long2ObjectHashMap<>();
  private final OneToOneConcurrentArrayQueue<Image> availableImages =
      new OneToOneConcurrentArrayQueue<>(1024);
  private final OneToOneConcurrentArrayQueue<Image> unavailableImages =
      new OneToOneConcurrentArrayQueue<>(1024);
  private final Function<Image, ResponseHandler> handlerFactory;
  private final int requestStreamId;
  private final int responseStreamId;
  private final ChannelUriStringBuilder requestUriBuilder;
  private final ChannelUriStringBuilder responseUriBuilder;
  private final ControlledFragmentAssembler requestAssembler =
      new ControlledFragmentAssembler(this::onControlledRequestMessage);

  private Subscription serverSubscription;

  public ResponseServer(
      final Aeron aeron,
      final Function<Image, ResponseHandler> handlerFactory,
      final String requestEndpoint,
      final int requestStreamId,
      final String responseControl,
      final int responseStreamId,
      final String requestChannel,
      final String responseChannel) {
    this.aeron = aeron;
    this.handlerFactory = handlerFactory;
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
      getOrCreateSession(image);
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
    clientToPublicationMap.values().forEach(CloseHelper::quietClose);
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
    ResponseSession session = getOrCreateSession((Image) header.context());

    return session.process(buffer, offset, length, header)
        ? ControlledFragmentHandler.Action.CONTINUE
        : ControlledFragmentHandler.Action.ABORT;
  }

  private ResponseSession getOrCreateSession(Image image) {
    ResponseSession session = clientToPublicationMap.get(image.correlationId());

    if (null == session) {
      Publication responsePublication =
          aeron.addPublication(
              responseUriBuilder.responseCorrelationId(image.correlationId()).build(),
              responseStreamId);

      ResponseHandler handler = handlerFactory.apply(image);
      session = new ResponseSession(responsePublication, handler);

      clientToPublicationMap.put(image.correlationId(), session);
    }

    return session;
  }

  private void removeSession(Image image) {
    requestAssembler.freeSessionBuffer(image.sessionId());
    ResponseSession session = clientToPublicationMap.remove(image.correlationId());
    CloseHelper.quietClose(session);
  }

  private static final class ResponseSession implements AutoCloseable {
    private final Publication publication;
    private final ResponseHandler handler;

    ResponseSession(Publication publication, ResponseHandler handler) {
      this.publication = publication;
      this.handler = handler;
    }

    public boolean process(DirectBuffer buffer, int offset, int length, Header header) {
      return handler.onMessage(buffer, offset, length, header, publication);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
      CloseHelper.close(publication);
    }
  }
}
