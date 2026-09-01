package stoufexis.jarpc.lib.util;

import stoufexis.jarpc.lib.model.ClaimHandle;
import stoufexis.jarpc.lib.model.ErrorCode;
import stoufexis.jarpc.lib.util.MPSCRingBuffer.Consume2;

import java.util.function.Consumer;

// Does not need to implement Agent, its used manually
public abstract class MPSCBufferPollAgent<T> {
  private final MPSCRingBuffer<T> ringBuffer;
  private final T scratch;
  private final Consume2<T, T> copy;
  private final OnCorruptPublication onCorruptPublication;

  private boolean populated = false;

  protected final ClaimHandle claimHandle = new ClaimHandle();

  protected MPSCBufferPollAgent(
      MPSCRingBuffer<T> ringBuffer,
      T scratch,
      Consume2<T, T> copy,
      OnCorruptPublication onCorruptPublication) {
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
      onCorruptPublication.onCorruptPublication(code);
      return true;
    }
  }

  protected abstract boolean processRequest(T scratch);

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
}
