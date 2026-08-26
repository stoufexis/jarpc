package stoufexis.jarpc.server;

import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import stoufexis.jarpc.model.MessageHeader;

public abstract class JarpcServer implements Agent, AutoCloseable {
  private static final int FRAGMENT_LIMIT = 10;

  private final MessageHeader header = new MessageHeader();
  private final ControlledFragmentAssembler assembled =
      new ControlledFragmentAssembler(this::onFragment);

  private final ServerErrorHandler errorHandler;
  private final Images images;
  private final int responseStreamId;
  private final ChannelUriStringBuilder responseUriBuilder;
  private final Subscription serverSubscription;
  private final Aeron aeron;
  private final ServerPublications publications;
  private final DecodeFailureUtil decodeFailureUtil;

  protected JarpcServer(
      ServerErrorHandler errorHandler,
      Images images,
      int responseStreamId,
      ChannelUriStringBuilder responseUriBuilder,
      Subscription serverSubscription,
      Aeron aeron) {
    this.errorHandler = errorHandler;
    this.images = images;
    this.responseStreamId = responseStreamId;
    this.responseUriBuilder = responseUriBuilder;
    this.serverSubscription = serverSubscription;
    this.aeron = aeron;
    this.publications = new ServerPublications();
    this.decodeFailureUtil = new DecodeFailureUtil(publications, errorHandler);
  }

  @Override
  public int doWork() {
    int work = 0;

    Image image;
    while (null != (image = images.pollAvailable())) {
      work++;
      ensurePublicationExists(image);
    }

    while (null != (image = images.pollUnavailable())) {
      work++;
      assembled.freeSessionBuffer(image.sessionId());
      CloseHelper.quietClose(publications.remove(image.correlationId()));
    }

    return work + serverSubscription.controlledPoll(assembled, FRAGMENT_LIMIT);
  }

  @Override
  public void close() {
    CloseHelper.quietClose(serverSubscription);
    publications.closeAll();
  }

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header aeronHeader) {
    Image image = (Image) aeronHeader.context();
    ensurePublicationExists(image);

    try {
      header.decode(buffer, offset, length);

      boolean result =
          onMessage(
              image.correlationId(),
              header.getMessageType(),
              header.getCorrelationId(),
              buffer,
              offset + MessageHeader.HEADER_SIZE,
              length - MessageHeader.HEADER_SIZE);

      return result
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      throw e;
    }
  }

  private void ensurePublicationExists(Image image) {
    // FIXME We dont need computeIfAbsent, put/remove only happen in the agent thread.
    if (null == publications.get(image.correlationId())) {
      Publication publication =
          aeron.addPublication(
              responseUriBuilder.responseCorrelationId(image.correlationId()).build(),
              responseStreamId);

      publications.put(image.correlationId(), publication);
    }
  }

  @Override
  public String roleName() {
    return "JarpcServerReceiver";
  }

  protected boolean sendDecodeFailure(long clientId, long correlationId, int baseMessageType) {
    return decodeFailureUtil.sendDecodeFailure(clientId, correlationId, baseMessageType);
  }

  protected Publication getPublication(long clientId) {
    return publications.get(clientId);
  }

  protected abstract boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length);
}
