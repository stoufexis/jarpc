package stoufexis.jarpc.util;

import jdk.internal.vm.annotation.Contended;

public final class SPSCRingBuffer<E> {

  private final E[] arr;
  private final int capacity;
  private final int mask;

  private final ProducerIndices producerIndices = new ProducerIndices();
  private final ConsumerIndices consumerIndices = new ConsumerIndices();

  @SuppressWarnings("unchecked")
  public SPSCRingBuffer(int capacity) {
    // capacity must be a power of 2
    // e.g. 4 -> 0100 & (0100 - 0001) = 0100 & 0011 = 0
    if ((capacity & (capacity - 1)) != 0b0) {
      throw new IllegalArgumentException("capacity must be power of 2");
    }
    this.capacity = capacity;
    this.mask = capacity - 1;
    this.arr = (E[]) new Object[capacity];
  }

  /**
   * Producer thread only. @return false if full (no blocking).
   */
  public boolean offer(E e) {
    long seq = producerIndices.producerSeq; // volatile read once

    if (seq - producerIndices.cachedConsumerSeq >= capacity) {
      // cachedConsumerSeq falls behind consumerSeq on purpose and is only refreshed from the
      // volatile var it shows no available space
      producerIndices.cachedConsumerSeq = consumerIndices.consumerSeq;

      if (seq - producerIndices.cachedConsumerSeq >= capacity) {
        // still no available space, cachedConsumerSeq was actually accurate
        return false;
      }
    }

    arr[index(seq)] = e;
    producerIndices.producerSeq = seq + 1;
    return true;
  }

  /**
   * Consumer thread only. @return null if empty (no blocking).
   */
  public E poll() {
    long seq = consumerIndices.consumerSeq;

    if (seq >= consumerIndices.cachedProducerSeq) {
      consumerIndices.cachedProducerSeq = producerIndices.producerSeq;

      if (seq >= consumerIndices.cachedProducerSeq) {
        return null;
      }
    }

    // touch the memory before increasing the consume index, otherwise the memory might be touched
    // by the producer concurrently
    E elem = arr[index(seq)];
    consumerIndices.consumerSeq = seq + 1;
    return elem;
  }

  // doesnt need manual inlining, private, needs no virtual dispatch, JIT will probably inline it
  private int index(long seq) {
    return (int) (seq & mask);
  }

  @Contended
  private static final class ProducerIndices {
    private volatile long producerSeq;
    private long cachedConsumerSeq;
  }

  @Contended
  private static final class ConsumerIndices {
    private volatile long consumerSeq;
    private long cachedProducerSeq;
  }
}

