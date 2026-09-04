package stoufexis.jarpc.lib.client.singlethread;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.client.concurrent.IsConnected;
import stoufexis.jarpc.lib.common.Poll;
import stoufexis.jarpc.lib.common.Publisher;
import stoufexis.jarpc.lib.common.internal.MessageHeaderCodec;

public abstract class SingleThreadedJarpcClient implements AutoCloseable, Poll, IsConnected {

  private final Publication publication;
  private final Subscription subscription;
  private final ControlledFragmentHandler fragmentHandler;
  private final ClientErrorHandler errorHandler;

  protected final Publisher publisher;

  protected SingleThreadedJarpcClient(
      Publication publication, Subscription subscription, ClientErrorHandler errorHandler) {
    this.fragmentHandler = new ControlledFragmentAssembler(this::onFragment);
    this.publication = publication;
    this.subscription = subscription;
    this.errorHandler = errorHandler;
    this.publisher = new Publisher(publication);
  }

  protected abstract boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length);

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header h_) {
    try {
      MessageHeaderCodec.assertSize(length);
      int messageType = MessageHeaderCodec.decodeMessageType(buffer, offset);
      long correlationId = MessageHeaderCodec.decodeCorrelationId(buffer, offset);

      offset += MessageHeaderCodec.HEADER_SIZE;
      length -= MessageHeaderCodec.HEADER_SIZE;

      return onMessage(messageType, correlationId, buffer, offset, length)
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      return ControlledFragmentHandler.Action.CONTINUE;
    }
  }

  @Override
  public final boolean isConnected() {
    return publication.isConnected() && subscription.isConnected();
  }

  @Override
  public final int poll(int limit) {
    return subscription.controlledPoll(fragmentHandler, limit);
  }

  @Override
  public final void close() {
    CloseHelper.quietCloseAll(publication, subscription);
  }
}
