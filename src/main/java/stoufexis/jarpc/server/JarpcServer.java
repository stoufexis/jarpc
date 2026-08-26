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
  private final Subscription serverSubscription;
  private final ServerPublications publications;
  private final DecodeFailureUtil decodeFailureUtil;

  protected JarpcServer(
      ServerErrorHandler errorHandler,
      Images images,
      int responseStreamId,
      String responseControl,
      Subscription serverSubscription,
      Aeron aeron) {
    this.errorHandler = errorHandler;
    this.images = images;
    this.serverSubscription = serverSubscription;
    this.publications = new ServerPublications(responseControl, aeron, responseStreamId);
    this.decodeFailureUtil = new DecodeFailureUtil(publications, errorHandler);
  }

  @Override
  public int doWork() {
    int work = 0;

    Image image;
    while (null != (image = images.pollAvailable())) {
      work++;
      publications.ensurePublicationExists(image);
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
    publications.ensurePublicationExists(image);

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

  @Override
  public String roleName() {
    return "JarpcServerReceiver";
  }

  /** Not thread-safe */
  protected boolean sendDecodeFailure(long clientId, long correlationId, int baseMessageType) {
    return decodeFailureUtil.sendDecodeFailure(clientId, correlationId, baseMessageType);
  }

  /** Thread-safe */
  protected Publication getPublication(long clientId) {
    return publications.get(clientId);
  }

  /** Will be called exclusively from the agent thread */
  protected abstract boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length);
}
