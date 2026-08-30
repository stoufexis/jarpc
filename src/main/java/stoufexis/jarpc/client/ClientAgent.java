package stoufexis.jarpc.client;

import org.agrona.concurrent.Agent;
import stoufexis.jarpc.util.Consume2;
import stoufexis.jarpc.util.MPSCRingBuffer;

public abstract class ClientAgent<T> implements Agent {
  private final MPSCRingBuffer<T> ringBuffer;
  private final T scratch;
  private final Consume2<T, T> copy;

  private boolean populated = false;

  protected ClientAgent(MPSCRingBuffer<T> ringBuffer, T scratch, Consume2<T, T> copy) {
    this.ringBuffer = ringBuffer;
    this.scratch = scratch;
    this.copy = copy;
  }

  protected abstract boolean processRequest(T scratch);

  @Override
  public final int doWork() {
    int work = 0;

    if (!populated) {
      populated = ringBuffer.poll(copy, scratch);
    }

    if (populated && processRequest(scratch)) {
      populated = false;
      work++;
    }

    return work;
  }

  @Override
  public final String roleName() {
    return "ClientAgent";
  }
}
