package stoufexis.jarpc.util;

import java.util.concurrent.atomic.AtomicLong;

// FIXME This implementation is fairly unoptimized. Theres false sharing, and not granular enough
// atomic operations

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MPSCRingBuffer<E> {

  private final E[] arr;
  private final AtomicLongArray readySeq;
  private final int capacity;
  private final int mask;

  private final AtomicLong producerSeq = new AtomicLong();
  private volatile long consumerSeq; // only the consumer thread writes this

  @SuppressWarnings("unchecked")
  public MPSCRingBuffer(int capacity, Supplier<E> factory) {
    // capacity must be a power of 2
    if ((capacity & (capacity - 1)) != 0) {
      throw new IllegalArgumentException("capacity must be power of 2");
    }
    this.capacity = capacity;
    this.mask = capacity - 1;
    this.arr = (E[]) new Object[capacity];
    this.readySeq = new AtomicLongArray(capacity);
    for (int i = 0; i < capacity; i++) {
      arr[i] = factory.get();
      readySeq.set(i, -1); // slot i's first real sequence is i, so -1 can never look "ready"
    }
  }

  public <B, C> boolean offer(Consume3<E, B, C> filler, B b, C c) {
    for (; ; ) {
      long seq = producerSeq.get();

      if (seq - consumerSeq >= capacity) {
        return false; // full
      }

      // Try to claim slot `seq`. If another producer beats us to it, reload and retry.
      if (producerSeq.compareAndSet(seq, seq + 1)) {
        int idx = index(seq);
        filler.accept(arr[idx], b, c);
        // Publish: this volatile write is what the consumer waits on, since two producers
        // can finish mutating their slots in a different order than they claimed them.
        readySeq.set(idx, seq);
        return true;
      }
    }
  }

  public <B> boolean poll(Consume2<E, B> processor, B b) {
    long seq = consumerSeq;
    int idx = index(seq);

    // If this slot isn't marked as holding `seq`'s data - whether because nothing's been
    // produced yet, or a producer has claimed it but not finished writing - there's simply
    // nothing to consume right now. No need to spin and disambiguate: either way the answer
    // is "not yet".
    if (readySeq.get(idx) != seq) {
      return false;
    }

    processor.accept(arr[idx], b);
    consumerSeq = seq + 1; // publishes progress to producers
    return true;
  }

  private int index(long seq) {
    return (int) (seq & mask);
  }
}
