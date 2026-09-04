package stoufexis.jarpc.lib.client.ringbuffer;

import static stoufexis.jarpc.lib.client.ringbuffer.DoubleReference.offer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.AgentTerminationException;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.common.ErrorCode;

public class MPMCBufferPollAgentTest {

  @Test
  void agent_consumes_buffer() {
    MPSCRingBuffer<DoubleReference> buf = new MPSCRingBuffer<>(2, DoubleReference::new);
    CompletableFuture<Void> fut = new CompletableFuture<>();

    var pollAgent =
        new MPSCBufferPollAgent<>(
            new DoubleReference(), DoubleReference::copy, new ErrorHandler(), buf, null) {
          int cnt = -1;

          @Override
          protected boolean process(DoubleReference scratch, OnError onError) {
            int expected = cnt + 1;

            if (expected != scratch.intA) {
              fut.completeExceptionally(
                  new AssertionFailedError("Expected " + expected + " got " + scratch.intA));

              throw new AgentTerminationException();
            }

            if (scratch.intA == 10) fut.complete(null);
            cnt = scratch.intA;
            return true;
          }
        };

    try (AgentRunner runner = runner(new FunctionAgent(pollAgent::doWork))) {
      AgentRunner.startOnThread(runner);

      for (int i = 0; i <= 10; ) if (offer(buf, i, 0)) i++;

      fut.get(10, TimeUnit.SECONDS);

    } catch (ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void agent_backpressures() {
    MPSCRingBuffer<DoubleReference> buf = new MPSCRingBuffer<>(2, DoubleReference::new);

    final CompletableFuture<List<Integer>> fut = new CompletableFuture<>();

    var pollAgent =
        new MPSCBufferPollAgent<>(
            new DoubleReference(), DoubleReference::copy, new ErrorHandler(), buf, null) {
          private LinkedList<Integer> integers = new LinkedList<>();
          private int cnt = 0;

          @Override
          protected boolean process(DoubleReference scratch, OnError onError) {
            if (scratch.intA == Integer.MAX_VALUE) {
              fut.complete(List.copyOf(integers));
              return true;
            }

            integers.add(scratch.intA);
            cnt++;
            return cnt % 3 == 0;
          }
        };

    try (AgentRunner runner = runner(new FunctionAgent(pollAgent::doWork))) {
      AgentRunner.startOnThread(runner);

      for (int i = 1; i <= 5; ) if (offer(buf, i, 0)) i++;

      while (!offer(buf, Integer.MAX_VALUE, 0)) {} // terminal signal

      List<Integer> result = fut.get(10, TimeUnit.SECONDS);

      Assertions.assertArrayEquals(
          new Integer[] {1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5}, result.toArray());

    } catch (ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static AgentRunner runner(Agent agent) {
    return new AgentRunner(new SleepingIdleStrategy(), _ -> {}, null, agent);
  }

  private record FunctionAgent(Supplier<Integer> work) implements Agent {
    @Override
    public int doWork() {
      return work.get();
    }

    @Override
    public String roleName() {
      return "";
    }
  }

  private static class ErrorHandler implements ClientErrorHandler {
    @Override
    public void onCallbackNotFound(long correlationId, String type) {}

    @Override
    public void onCorruptPublication(ErrorCode code) {}

    @Override
    public void onError(Throwable throwable) {}
  }
}
