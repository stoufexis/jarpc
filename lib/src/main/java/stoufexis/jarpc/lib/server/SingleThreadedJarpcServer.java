package stoufexis.jarpc.lib.server;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.lib.common.ConnectionConfig;
import stoufexis.jarpc.lib.common.Poll;
import stoufexis.jarpc.lib.common.internal.MessageHeaderCodec;
import stoufexis.jarpc.lib.server.internal.ServerPublications;

public abstract class SingleThreadedJarpcServer implements AutoCloseable, Poll {
  protected final ServerPublications publications;
  private final Images images;
  private final Subscription subscription;
  private final ServerStateMachine serverStateMachine;

  private final ControlledFragmentAssembler assembled =
      new ControlledFragmentAssembler(this::onFragment);

  protected SingleThreadedJarpcServer(
      Subscription subscription,
      Images images,
      ServerStateMachine serverStateMachine,
      ConnectionConfig cfg) {
    this.subscription = subscription;
    this.publications =
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId());
    this.images = images;
    this.serverStateMachine = serverStateMachine;
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
      serverStateMachine.onClientDisconnected(image.correlationId());
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
        serverStateMachine.onProcessingError(clientId, correlationId, messageType);
      }

      return result
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      serverStateMachine.onError(e);
      throw e;
    }
  }
}
