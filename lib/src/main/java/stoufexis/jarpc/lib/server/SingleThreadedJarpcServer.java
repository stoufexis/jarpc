package stoufexis.jarpc.lib.server;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.lib.model.ClientHook;
import stoufexis.jarpc.lib.model.MessageHeaderCodec;
import stoufexis.jarpc.lib.model.Poll;

public abstract class SingleThreadedJarpcServer implements AutoCloseable, Poll {
  private final ServerPublications publications;
  private final Images images;
  private final Subscription subscription;
  private final ClientHook clientHook;
  private final ServerErrorHandler errorHandler;

  private final ControlledFragmentAssembler assembled =
      new ControlledFragmentAssembler(this::onFragment);

  protected SingleThreadedJarpcServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,
      ClientHook clientHook,
      ServerErrorHandler errorHandler) {
    this.subscription = subscription;
    this.publications = publications;
    this.images = images;
    this.clientHook = clientHook;
    this.errorHandler = errorHandler;
  }

  @Override
  public final int poll(int limit) {
    int work = 0;

    Image image;
    while (null != (image = images.pollAvailable())) {
      work++;
      publications.ensurePublicationExists(image.correlationId());
    }

    while (null != (image = images.pollUnavailable())) {
      work++;
      assembled.freeSessionBuffer(image.sessionId());
      CloseHelper.quietClose(publications.remove(image.correlationId()));
      clientHook.onClientDisconnected(image.correlationId());
    }

    return work + subscription.controlledPoll(assembled, limit);
  }

  @Override
  public final void close() {
    CloseHelper.quietClose(subscription);
    publications.closeAll();
  }

  protected abstract boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length);

  protected final Publication getPublication(long clientId) {
    return publications.get(clientId);
  }

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header aeronHeader) {
    try {
      long clientId = ((Image) aeronHeader.context()).correlationId();
      publications.ensurePublicationExists(clientId);

      MessageHeaderCodec.assertSize(length);
      int messageType = MessageHeaderCodec.decodeMessageType(buffer, offset);
      long correlationId = MessageHeaderCodec.decodeCorrelationId(buffer, offset);

      offset += MessageHeaderCodec.HEADER_SIZE;
      length -= MessageHeaderCodec.HEADER_SIZE;

      boolean result;
      try {
        result = onMessage(clientId, messageType, correlationId, buffer, offset, length);
      } catch (RuntimeException e) {
        result = true;
        errorHandler.onProcessingError(clientId, correlationId, messageType);
      }

      return result
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      throw e;
    }
  }
}
