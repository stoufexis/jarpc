package stoufexis.jarpc.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.CloseHelper;

public abstract class JarpcClient implements AutoCloseable {

  protected final Publication publication;

  private final Subscription subscription;
  private final PollFragmentHandler handler;

  protected JarpcClient(
      Publication publication, Subscription subscription, PollFragmentHandler handler) {
    this.handler = handler;
    this.publication = publication;
    this.subscription = subscription;
  }

  protected int poll(int limit) {
    return subscription.controlledPoll(handler.getFragmentHandler(), limit);
  }

  @Override
  public final void close() throws Exception {
    CloseHelper.quietCloseAll(publication, subscription);
  }
}
