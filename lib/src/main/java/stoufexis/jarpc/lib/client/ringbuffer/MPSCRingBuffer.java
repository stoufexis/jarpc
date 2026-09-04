package stoufexis.jarpc.lib.client.ringbuffer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Supplier;

/**
 * Use this instead of a ManyToOneRingBuffer if you need to pass objects that cannot be serialized,
 * e.g. callbacks.
 */
final class MPSCRingBuffer<E> {

  // FIXME This implementation is fairly unoptimized. Theres false sharing, and not granular enough
  // atomic operations

  private final E[] arr;
  private final AtomicLongArray readySeq;
  private final int capacity;
  private final int mask;

  private final AtomicLong producerSeq = new AtomicLong();
  private volatile long consumerSeq = 0;

  MPSCRingBuffer(int capacity, Supplier<E> factory) {
    if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
      throw new IllegalArgumentException("capacity must be a positive power of 2. Was " + capacity);
    }
    this.capacity = capacity;
    this.mask = capacity - 1;
    //noinspection unchecked
    this.arr = (E[]) new Object[capacity];
    this.readySeq = new AtomicLongArray(capacity);
    for (int i = 0; i < capacity; i++) {
      arr[i] = factory.get();
      readySeq.set(i, -1); // slot i's first real sequence is i
    }
  }

  /** Filler must never throw */
  <B, C> boolean offer(Consume3<E, B, C> filler, B b, C c) {
    for (; ; ) {
      long seq = producerSeq.get();

      if (seq - consumerSeq >= capacity) {
        return false; // full
      }

      if (producerSeq.compareAndSet(seq, seq + 1)) {
        int idx = index(seq);
        filler.accept(arr[idx], b, c);
        readySeq.set(idx, seq);
        return true;
      }
    }
  }

  /** Copy must never throw */
  boolean poll(Consume2<E, E> copy, E b) {
    long seq = consumerSeq;
    int idx = index(seq);

    if (readySeq.get(idx) != seq) {
      return false;
    }

    copy.accept(b, arr[idx]);
    consumerSeq = seq + 1; // publishes progress to producers
    return true;
  }

  private int index(long seq) {
    return (int) (seq & mask);
  }
}
