package stoufexis.jarpc.lib.client;

import stoufexis.jarpc.lib.client.MPSCRingBuffer.Consume2;
import stoufexis.jarpc.lib.common.ClaimHandle;
import stoufexis.jarpc.lib.common.ErrorCode;

// Does not need to implement Agent, its used manually
public abstract class MPSCBufferPollAgent<T> {
  private final MPSCRingBuffer<T> ringBuffer;
  private final T scratch;
  private final Consume2<T, T> copy;
  private final ClientErrorHandler errorHandler;

  private boolean populated = false;
  protected final ClaimHandle claimHandle = new ClaimHandle();

  protected MPSCBufferPollAgent(
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

  protected abstract boolean process(T scratch);

  public final int doWork() {
    int work = 0;

    if (!populated) {
      populated = ringBuffer.poll(copy, scratch);
      work++;
    }

    if (populated && process(scratch)) {
      claimHandle.commit();
      populated = false;
      work++;
    }

    return work;
  }
}
