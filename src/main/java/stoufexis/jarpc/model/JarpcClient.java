package stoufexis.jarpc.model;

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
import stoufexis.jarpc.util.ClassAgent;
import stoufexis.jarpc.util.MessageHeader;

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
   * @throws RuntimeException in case decoding/dispatching fails
   */
  protected abstract boolean handleReceivedFragment(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length);

  private class ReceiveAgent extends ClassAgent {
    private final MessageHeader header = new MessageHeader();

    private final ControlledFragmentHandler assembled =
        new ControlledFragmentAssembler(this::onFragment);

    private int work;

    @Override
    public int doWork() {
      work = 0;
      subscription.controlledPoll(assembled, FRAGMENT_LIMIT);
      return work;
    }

    private ControlledFragmentHandler.Action onFragment(
        DirectBuffer buffer, int offset, int length, Header h_) {
      try {
        header.decode(buffer, offset, length);

        boolean dispatchResult =
            handleReceivedFragment(
                header.getMessageType(),
                header.getCorrelationId(),
                buffer,
                offset + MessageHeader.HEADER_SIZE,
                length - MessageHeader.HEADER_SIZE);

        // deliberately accounts 1 point for each post-assembled fragment
        // and 1 point for backpressure
        work++;

        return dispatchResult
            ? ControlledFragmentHandler.Action.CONTINUE
            : ControlledFragmentHandler.Action.ABORT;

      } catch (RuntimeException e) {
        handler.onError(e);
        return ControlledFragmentHandler.Action.CONTINUE;
      }
    }
  }
}
