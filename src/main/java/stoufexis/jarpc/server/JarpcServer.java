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
  private final ProcessingFailureUtil processingFailureUtil;

  protected JarpcServer(
      Aeron aeron,
      ServerErrorHandler errorHandler,
      Images images,
      int responseStreamId,
      String responseControl,
      Subscription serverSubscription) {
    this.errorHandler = errorHandler;
    this.images = images;
    this.serverSubscription = serverSubscription;
    this.publications = new ServerPublications(responseControl, aeron, responseStreamId);
    this.processingFailureUtil = new ProcessingFailureUtil(errorHandler);
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
  public String roleName() {
    return "JarpcServerReceiver";
  }

  @Override
  public void close() {
    CloseHelper.quietClose(serverSubscription);
    publications.closeAll();
  }

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header aeronHeader) {
    try {
      Image image = (Image) aeronHeader.context();
      Publication publication = publications.ensurePublicationExists(image);

      header.decode(buffer, offset, length);

      long clientId = image.correlationId();
      int messageType = header.getMessageType();
      long correlationId = header.getCorrelationId();

      boolean result;
      try {
        result =
            onMessage(
                clientId,
                messageType,
                correlationId,
                buffer,
                offset + MessageHeader.HEADER_SIZE,
                length - MessageHeader.HEADER_SIZE);
      } catch (RuntimeException e) {
        result =
            processingFailureUtil.sendProcessingFailure(
                publication, clientId, correlationId, /*last:*/ true, messageType);
      }

      return result
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      throw e;
    }
  }

  /**
   * Will be called exclusively from the agent thread
   *
   * @throws RuntimeException if decoding fails.
   */
  protected abstract boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length);

  protected abstract class ServerCallback {

    /** Thread-safe */
    protected Publication getPublication(long clientId) {
      return publications.get(clientId);
    }
  }
}
