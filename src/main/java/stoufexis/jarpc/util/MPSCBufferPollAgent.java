package stoufexis.jarpc.util;

import org.agrona.concurrent.Agent;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.MPSCRingBuffer.Consume2;

import java.util.function.Consumer;

public abstract class MPSCBufferPollAgent<T> implements Agent {
  private final MPSCRingBuffer<T> ringBuffer;
  private final T scratch;
  private final Consume2<T, T> copy;
  private final Consumer<ErrorCode> onCorruptPublication;

  private boolean populated = false;

  protected final ClaimHandle claimHandle = new ClaimHandle();

  protected MPSCBufferPollAgent(
      MPSCRingBuffer<T> ringBuffer,
      T scratch,
      Consume2<T, T> copy,
      Consumer<ErrorCode> onCorruptPublication) {
    this.ringBuffer = ringBuffer;
    this.scratch = scratch;
    this.copy = copy;
    this.onCorruptPublication = onCorruptPublication;
  }

  protected final boolean handleError() {
    ErrorCode code = claimHandle.getCode();

    if (code == ErrorCode.BACKPRESSURE) {
      return false;
    } else {
      onCorruptPublication.accept(code);
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
