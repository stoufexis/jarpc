package stoufexis.jarpc.lib.client.ringbuffer;

import java.util.function.Supplier;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.common.ClaimHandle;
import stoufexis.jarpc.lib.common.ErrorCode;

/** Does not need to implement Agent, its used manually. */
public abstract class MPSCBufferPollAgent<T> {

  public interface OnError {
    boolean run();
  }

  private final MPSCRingBuffer<T> ringBuffer;
  private final T scratch;
  private final Consume2<T, T> copy;
  private final ClientErrorHandler errorHandler;
  private final OnError onError;

  private boolean populated = false;
  protected final ClaimHandle claimHandle;

  protected MPSCBufferPollAgent(
      Supplier<T> factory,
      Consume2<T, T> copy,
      ClientErrorHandler errorHandler,
      int queueCapacity) {
    this(
        factory.get(),
        copy,
        errorHandler,
        new MPSCRingBuffer<>(queueCapacity, factory),
        new ClaimHandle());
  }

  MPSCBufferPollAgent(
      T scratch,
      Consume2<T, T> copy,
      ClientErrorHandler errorHandler,
      MPSCRingBuffer<T> buffer,
      ClaimHandle claimHandle) {
    this.scratch = scratch;
    this.ringBuffer = buffer;
    this.copy = copy;
    this.errorHandler = errorHandler;
    this.onError = this::handleError;
    this.claimHandle = claimHandle;
  }

  protected abstract boolean process(T scratch, OnError onError);

  public final <B, C> boolean offer(Consume3<T, B, C> filler, B b, C c) {
    return ringBuffer.offer(filler, b, c);
  }

  public final int doWork() {
    int work = 0;

    // if the previous process returned false and our scratch buffer is still full, do not overwrite
    // it
    if (!populated) {
      populated = ringBuffer.poll(copy, scratch);
      work++;
    }

    if (populated && process(scratch, onError)) {
      if (claimHandle != null) claimHandle.commit();
      populated = false;
      work++;
    }

    return work;
  }

  private boolean handleError() {
    ErrorCode code = claimHandle.getCode();

    if (code == ErrorCode.BACKPRESSURE) {
      return false;
    } else {
      errorHandler.onCorruptPublication(code);
      return true;
    }
  }
}
