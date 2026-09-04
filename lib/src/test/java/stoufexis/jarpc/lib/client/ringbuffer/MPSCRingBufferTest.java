package stoufexis.jarpc.lib.client.ringbuffer;

import static org.junit.jupiter.api.Assertions.*;
import static stoufexis.jarpc.lib.client.ringbuffer.DoubleReference.*;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class MPSCRingBufferTest {
  @Test
  void poll_reads_successful_offers() {
    var scratch = new DoubleReference();
    var buf = new MPSCRingBuffer<>(2, DoubleReference::new);

    // fill up to capacity
    assertTrue(offer(buf, 1, 10));
    assertTrue(offer(buf, 2, 20));
    assertFalse(offer(buf, 3, 30));

    // release one
    assertTrue(poll(buf, scratch));
    assertScratch(1, 10, scratch);

    // fill up to capacity
    assertTrue(offer(buf, 3, 30));
    assertFalse(offer(buf, 4, 40));

    assertTrue(poll(buf, scratch));
    assertScratch(2, 20, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(3, 30, scratch);

    // poll failed, scratch remains unchanged
    assertFalse(poll(buf, scratch));
    assertScratch(3, 30, scratch);
  }

  @Test
  void wraps_around_correctly() {
    var scratch = new DoubleReference();
    var buf = new MPSCRingBuffer<>(4, DoubleReference::new);

    assertTrue(offer(buf, 1, 10));
    assertTrue(offer(buf, 2, 20));
    assertTrue(offer(buf, 3, 30));

    assertTrue(poll(buf, scratch));
    assertScratch(1, 10, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(2, 20, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(3, 30, scratch);

    assertFalse(poll(buf, scratch));

    assertTrue(offer(buf, 4, 40));
    assertTrue(offer(buf, 5, 50));
    assertTrue(offer(buf, 6, 60));

    assertTrue(poll(buf, scratch));
    assertScratch(4, 40, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(5, 50, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(6, 60, scratch);

    assertFalse(poll(buf, scratch));

    assertTrue(offer(buf, 7, 70));
    assertTrue(offer(buf, 8, 80));
    assertTrue(offer(buf, 9, 90));

    assertTrue(poll(buf, scratch));
    assertScratch(7, 70, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(8, 80, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(9, 90, scratch);

    assertFalse(poll(buf, scratch));

    assertTrue(offer(buf, 10, 100));
    assertTrue(offer(buf, 11, 110));
    assertTrue(offer(buf, 12, 120));

    assertTrue(poll(buf, scratch));
    assertScratch(10, 100, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(11, 110, scratch);

    assertTrue(poll(buf, scratch));
    assertScratch(12, 120, scratch);

    assertFalse(poll(buf, scratch));
  }

  @RepeatedTest(10)
  void concurrent_producers_synchronize() {
    concurrent_producers_synchronize_impl(10, false);
    concurrent_producers_synchronize_impl(100, false);
    concurrent_producers_synchronize_impl(10, true);
  }

  private static void concurrent_producers_synchronize_impl(int producers, boolean slowConsumer) {
    int countLimit = slowConsumer ? 2_000 : 20_000;
    long timeout = 30_000_000_000L;
    var buf = new MPSCRingBuffer<>(16, DoubleReference::new);
    var latch = new CountDownLatch(1);

    Function<Integer, Runnable> producer =
        id ->
            () -> {
              try {
                latch.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
              }

              for (int i = 0; i <= countLimit; ) {
                if (Thread.currentThread().isInterrupted()) {
                  System.out.println("Producer " + id + " interrupted");
                  break;
                }

                if (offer(buf, id, i)) i++;
              }
            };

    ExecutorService executor = Executors.newFixedThreadPool(producers);

    try {
      var counts = new ArrayList<Integer>(producers);

      for (int i = 0; i < producers; i++) {
        executor.submit(producer.apply(i));
        counts.add(i, -1);
      }

      // signal start
      latch.countDown();

      long start = System.nanoTime();
      var scratch = new DoubleReference();
      int remaining = producers;

      while (!Thread.currentThread().isInterrupted() && remaining > 0) {
        // timeout
        assertTrue(System.nanoTime() - start < timeout);

        if (poll(buf, scratch)) {
          int id = scratch.intA;
          int payload = scratch.intB;

          assertTrue(payload <= countLimit);
          assertEquals(counts.get(id) + 1, payload);
          counts.set(id, payload);

          if (payload == countLimit) remaining--;

          if (slowConsumer) LockSupport.parkNanos(50_000);
        }
      }
    } finally {
      executor.shutdownNow();
      executor.close();
    }
  }
}
