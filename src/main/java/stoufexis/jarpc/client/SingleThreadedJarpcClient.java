package stoufexis.jarpc.client;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import stoufexis.jarpc.model.MessageHeaderCodec;
import stoufexis.jarpc.model.Poll;
import stoufexis.jarpc.model.ProcessingFailureCodec;

import static stoufexis.jarpc.util.Util.illegal;

public abstract class SingleThreadedJarpcClient implements AutoCloseable, Poll {

  private final Publication publication;
  private final Subscription subscription;
  private final ControlledFragmentHandler fragmentHandler;
  private final ErrorHandler errorHandler;

  protected final Publisher publisher;

  protected SingleThreadedJarpcClient(
      Publication publication, Subscription subscription, ErrorHandler errorHandler) {
    this.fragmentHandler = new ControlledFragmentAssembler(this::onFragment);
    this.publication = publication;
    this.subscription = subscription;
    this.errorHandler = errorHandler;
    this.publisher = new Publisher(publication);
  }

  protected abstract boolean onProcessingFailure(int baseMessageType, long correlationId);

  protected abstract boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length);

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header header) {
    try {
      MessageHeaderCodec.assertSize(length);
      int messageType = MessageHeaderCodec.decodeMessageType(buffer, offset);
      long correlationId = MessageHeaderCodec.decodeCorrelationId(buffer, offset);

      offset += MessageHeaderCodec.HEADER_SIZE;
      length -= MessageHeaderCodec.HEADER_SIZE;

      boolean accepted;

      if (messageType > 0) {
        accepted = onMessage(messageType, correlationId, buffer, offset, length);
      } else if (messageType == ProcessingFailureCodec.MESSAGE_TYPE_ID) {
        ProcessingFailureCodec.assertSize(length);
        int baseMessageType = ProcessingFailureCodec.decodeBaseMessageType(buffer, offset);
        accepted = onProcessingFailure(baseMessageType, correlationId);
      } else {
        throw illegal("Unknown message type " + messageType);
      }

      return accepted
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      return ControlledFragmentHandler.Action.CONTINUE;
    }
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
