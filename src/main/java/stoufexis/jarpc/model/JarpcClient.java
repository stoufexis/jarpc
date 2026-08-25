package stoufexis.jarpc.model;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import stoufexis.jarpc.util.ClassAgent;

public abstract class JarpcClient extends ClassAgent {
  private static final int FRAGMENT_LIMIT = 10;

  protected final Publication publication;
  protected final Subscription subscription;
  protected final ErrorHandler handler;

  private final ControlledFragmentHandler assembled =
      new ControlledFragmentAssembler(this::onFragment);

  private int work;

  protected JarpcClient(Publication publication, Subscription subscription, ErrorHandler handler) {
    this.publication = publication;
    this.subscription = subscription;
    this.handler = handler;
  }

  protected abstract boolean handleReceivedFragment(DirectBuffer buffer, int offset, int length);

  @Override
  public int doWork() {
    work = 0;
    subscription.controlledPoll(assembled, FRAGMENT_LIMIT);
    return work;
  }

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header header) {
    boolean dispatchResult = handleReceivedFragment(buffer, offset, length);

    // deliberately accounts 1 point for each post-assembled fragment
    // and 1 point for backpressure
    work++;

    return dispatchResult
        ? ControlledFragmentHandler.Action.CONTINUE
        : ControlledFragmentHandler.Action.ABORT;
  }

  public final Thread startOnThread(IdleStrategy idleStrategy) {
    return AgentRunner.startOnThread(new AgentRunner(idleStrategy, handler, null, this));
  }
}
