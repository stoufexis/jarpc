package stoufexis.jarpc.lib.util;

import java.util.concurrent.atomic.AtomicLong;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Supplier;

public final class MPSCRingBuffer<E> {

  // FIXME This implementation is fairly unoptimized. Theres false sharing, and not granular enough
  // atomic operations

  public interface Consume2<A, B> {
    void accept(A a, B b);
  }

  public interface Consume3<A, B, C> {
    void accept(A a, B b, C c);
  }

  public interface Consume4Long<A, B> {
    void accept(A a, B b, long longC, long longD);
  }

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

  public <B> boolean offer(Consume4Long<E, B> filler, B b, long longC, long longD) {
    for (; ; ) {
      long seq = producerSeq.get();

      if (seq - consumerSeq >= capacity) {
        return false; // full
      }

      // Try to claim slot `seq`. If another producer beats us to it, reload and retry.
      if (producerSeq.compareAndSet(seq, seq + 1)) {
        int idx = index(seq);
        filler.accept(arr[idx], b, longC, longD);
        // Publish: this volatile write is what the consumer waits on, since two producers
        // can finish mutating their slots in a different order than they claimed them.
        readySeq.set(idx, seq);
        return true;
      }
    }
  }

  public <B> boolean poll(Consume2<B, E> copy, B b) {
    long seq = consumerSeq;
    int idx = index(seq);

    // If this slot isn't marked as holding `seq`'s data - whether because nothing's been
    // produced yet, or a producer has claimed it but not finished writing - there's simply
    // nothing to consume right now. No need to spin and disambiguate: either way the answer
    // is "not yet".
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
