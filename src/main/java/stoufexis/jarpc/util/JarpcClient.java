package stoufexis.jarpc.util;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import stoufexis.jarpc.model.BaseCatalog;
import stoufexis.jarpc.model.DecodeFailureResponse;

import java.nio.ByteBuffer;

import static stoufexis.jarpc.util.Util.illegal;

public abstract class JarpcClient {
  private static final int FRAGMENT_LIMIT = 10;

  protected final Publication publication;
  protected final Subscription subscription;
  protected final ErrorHandler handler;
  private final Agent agent = new ReceiveAgent();

  protected JarpcClient(Publication publication, Subscription subscription, ErrorHandler handler) {
    this.publication = publication;
    this.subscription = subscription;
    this.handler = handler;
  }

  public final Agent getAgent() {
    return agent;
  }

  public final Thread startOnThread(IdleStrategy idleStrategy) {
    return AgentRunner.startOnThread(new AgentRunner(idleStrategy, handler, null, agent));
  }

  /**
   * Runs in the dedicated Agent thread, can use mutable state. Should not share thread-unsafe state
   * with other methods of the class.
   *
   * @throws RuntimeException in case dispatching fails
   */
  protected abstract boolean handleReceivedFragment(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length);

  /**
   * Runs in the dedicated Agent thread, can use mutable state. Should not share thread-unsafe state
   * with other methods of the class.
   *
   * @throws RuntimeException in case dispatching fails
   */
  protected abstract void handleDecodeFailureResponse(
      int messageType, long correlationId, ByteBuffer bytes, int bytesSize);

  private class ReceiveAgent extends ClassAgent implements ControlledFragmentHandler {
    private final MessageHeader header = new MessageHeader();
    private final DecodeFailureResponse decodeFailureResponse = new DecodeFailureResponse();
    private final ControlledFragmentHandler assembled = new ControlledFragmentAssembler(this);

    @Override
    public int doWork() {
      return subscription.controlledPoll(assembled, FRAGMENT_LIMIT);
    }

    @Override
    public ControlledFragmentHandler.Action onFragment(
        DirectBuffer buffer, int offset, int length, Header h_) {
      try {
        header.decode(buffer, offset, length);

        int messageType = header.getMessageType();
        long correlationId = header.getCorrelationId();

        if (messageType > 0) {
          boolean dispatchResult =
              handleReceivedFragment(
                  messageType,
                  correlationId,
                  buffer,
                  offset + MessageHeader.HEADER_SIZE,
                  length - MessageHeader.HEADER_SIZE);

          return dispatchResult
              ? ControlledFragmentHandler.Action.CONTINUE
              : ControlledFragmentHandler.Action.ABORT;
        }

        switch (messageType) {
          case BaseCatalog.decodeFailure -> {
            decodeFailureResponse.decode(buffer, offset, length);

            handleDecodeFailureResponse(
                decodeFailureResponse.getBaseMessageType(),
                correlationId,
                decodeFailureResponse.getBytes(),
                decodeFailureResponse.getBytesSize());

            return ControlledFragmentHandler.Action.CONTINUE;
          }
          default -> throw illegal("Unknown failure message type " + messageType);
        }
      } catch (RuntimeException e) {
        handler.onError(e);
        return ControlledFragmentHandler.Action.CONTINUE;
      }
    }
  }
}
