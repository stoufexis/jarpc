package stoufexis.jarpc.client;

import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.Agent;
import stoufexis.jarpc.model.BaseCatalog;
import stoufexis.jarpc.model.ProcessingFailureResponse;
import stoufexis.jarpc.model.MessageHeader;

import static stoufexis.jarpc.util.Util.illegal;

public abstract class JarpcClient implements AutoCloseable, Agent {
  private static final int FRAGMENT_LIMIT = 10;

  private final Publication publication;
  private final Subscription subscription;
  private final ErrorHandler handler;
  private final ControlledFragmentHandler assembled;

  private final MessageHeader header = new MessageHeader();
  private final ProcessingFailureResponse processingFailureResponse =
      new ProcessingFailureResponse();

  protected JarpcClient(Publication publication, Subscription subscription, ErrorHandler handler) {
    this.publication = publication;
    this.subscription = subscription;
    this.handler = handler;
    this.assembled =
        new ControlledFragmentAssembler(
            (buffer, offset, length, _) ->
                onMessage(buffer, offset, length)
                    ? ControlledFragmentHandler.Action.CONTINUE
                    : ControlledFragmentHandler.Action.ABORT);
  }

  public boolean isConnected() {
    return publication.isConnected() && subscription.isConnected();
  }

  @Override
  public int doWork() {
    return subscription.controlledPoll(assembled, FRAGMENT_LIMIT);
  }

  @Override
  public String roleName() {
    return "JarpcClientReceiver";
  }

  @Override
  public void close() {
    CloseHelper.quietCloseAll(publication, subscription);
  }

  private boolean onMessage(DirectBuffer buffer, int offset, int length) {
    try {
      header.decode(buffer, offset, length);

      int messageType = header.getMessageType();
      int correlationId = header.getCorrelationId();
      boolean last = header.isLast();

      if (messageType > 0) {
        return handleReceivedFragment(
            messageType,
            correlationId,
            last,
            buffer,
            offset + MessageHeader.HEADER_SIZE,
            length - MessageHeader.HEADER_SIZE);

      } else if (messageType == BaseCatalog.processingFailure) {
        processingFailureResponse.decode(buffer, offset, length);
        handleProcessingFailureResponse(
            processingFailureResponse.getBaseMessageType(), correlationId, last);
        return true;

      } else {
        throw illegal("Unknown failure message type " + messageType);
      }

    } catch (RuntimeException e) {
      handler.onError(e);
      return true;
    }
  }

  /**
   * Runs in the dedicated Agent thread, can use mutable state. Should not share thread-unsafe state
   * with other methods of the class.
   *
   * @throws RuntimeException in case dispatching fails
   */
  protected abstract boolean handleReceivedFragment(
      int messageType,
      int correlationId,
      boolean last,
      DirectBuffer buffer,
      int offset,
      int length);

  /**
   * Runs in the dedicated Agent thread, can use mutable state. Should not share thread-unsafe state
   * with other methods of the class.
   *
   * @throws RuntimeException in case dispatching fails
   */
  protected abstract void handleProcessingFailureResponse(
      int messageType, int correlationId, boolean last);
}
