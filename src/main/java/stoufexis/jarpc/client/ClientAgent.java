package stoufexis.jarpc.client;

import org.agrona.concurrent.Agent;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.Consume2;
import stoufexis.jarpc.util.MPSCRingBuffer;

public abstract class ClientAgent<T> implements Agent {
  private final MPSCRingBuffer<T> ringBuffer;
  private final T scratch;
  private final Consume2<T, T> copy;
  private final ClientErrorHandler errorHandler;

  private boolean populated = false;

  protected final ClaimHandle claimHandle = new ClaimHandle();

  protected ClientAgent(
      MPSCRingBuffer<T> ringBuffer,
      T scratch,
      Consume2<T, T> copy,
      ClientErrorHandler errorHandler) {
    this.ringBuffer = ringBuffer;
    this.scratch = scratch;
    this.copy = copy;
    this.errorHandler = errorHandler;
  }

  protected final boolean handleError() {
    ErrorCode code = claimHandle.getCode();

    if (code == ErrorCode.BACKPRESSURE) {
      return false;
    } else {
      errorHandler.onCorruptPublication(code);
      return true;
    }
  }

  protected abstract boolean processRequest(T scratch);

  @Override
  public final int doWork() {
    int work = 0;

    if (!populated) {
      populated = ringBuffer.poll(copy, scratch);
    }

    if (populated && processRequest(scratch)) {
      claimHandle.commit();
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
