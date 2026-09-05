package stoufexis.jarpc.lib.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.common.Bytes;
import stoufexis.jarpc.lib.common.ClaimHandle;
import stoufexis.jarpc.lib.common.ConnectionConfig;
import stoufexis.jarpc.lib.common.ErrorCode;
import stoufexis.jarpc.lib.integration.generated.client.EchoConcurrentClient;
import stoufexis.jarpc.lib.integration.generated.client.EchoConcurrentJarpcClient;
import stoufexis.jarpc.lib.integration.generated.common.EchoRequestDecode;
import stoufexis.jarpc.lib.integration.generated.common.EchoResponseDecode;
import stoufexis.jarpc.lib.integration.generated.common.EchoResponseEncode;
import stoufexis.jarpc.lib.integration.generated.server.EchoSingleThreadedJarpcServer;
import stoufexis.jarpc.lib.integration.generated.server.EchoSingleThreadedServer;
import stoufexis.jarpc.lib.integration.generated.server.EchoSingleThreadedStateMachine;

public class IntegrationTest {

  @Test
  void smoke_test() {
    ConnectionConfig cfg =
        new ConnectionConfig("aeron:ipc", 1, "aeron:ipc", 2, ConnectionConfig.Media.IPC);

    try (MediaDriver mediaDriver =
            MediaDriver.launchEmbedded(
                new MediaDriver.Context()
                    .dirDeleteOnStart(true)
                    .threadingMode(ThreadingMode.SHARED)
                    .sharedIdleStrategy(new SleepingIdleStrategy())
                    .dirDeleteOnShutdown(true));
        //
        Aeron aeron =
            Aeron.connect(
                new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));
        //
        EchoServerAgent server = new EchoServerAgent(aeron, cfg);
        //
        EchoConcurrentJarpcClient client =
            EchoConcurrentJarpcClient.create(aeron, cfg, new ErrorHandler(), 1024);
        //
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        //
        AgentRunner runner = runner(new CompositeAgent(server, client))) {

      AgentRunner.startOnThread(runner);
      runSmokeTest(client, executorService);
    }
  }

  private static void runSmokeTest(
      EchoConcurrentJarpcClient client, ExecutorService executorService) {

    while (!client.isConnected()) LockSupport.parkNanos(100_000_000);

    int concurrency = 50_000;
    AtomicInteger succeeded = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();

    for (int i = 0; i < concurrency; i++) {
      final int param = i;

      executorService.submit(
          () -> {
            try {
              send(client, param).get(10, TimeUnit.SECONDS);
              succeeded.incrementAndGet();

            } catch (ExecutionException e) {
              // send fails for exactly half of the requests
              IO.println(param + " failed assertion " + e);
              failed.incrementAndGet();

            } catch (InterruptedException e) {
              IO.println(param + " interrupted");
              Thread.currentThread().interrupt();
              failed.incrementAndGet();

            } catch (TimeoutException e) {
              IO.println(param + " timed out");
              failed.incrementAndGet();

            } catch (RuntimeException e) {
              IO.println(param + " some other exception " + e);
            }
          });
    }

    while (succeeded.get() + failed.get() < concurrency) {
      LockSupport.parkNanos(100_000_000);
    }

    assertEquals(concurrency, succeeded.get());
    assertEquals(0, failed.get());
  }

  private static AgentRunner runner(Agent agent) {
    return new AgentRunner(new SleepingIdleStrategy(), _ -> {}, null, agent);
  }

  private static CompletableFuture<Void> send(EchoConcurrentJarpcClient client, int i) {
    var fut = new CompletableFuture<Void>();

    var request =
        new EchoRequestDecode() {
          @Override
          public boolean bool() {
            return i % 2 == 0;
          }

          @Override
          public byte bite() {
            return (byte) i;
          }

          @Override
          public short sort() {
            return (short) i;
          }

          @Override
          public int eent() {
            return i;
          }

          @Override
          public long log() {
            return (((long) i) << 32) | (i & 0xffffffffL);
          }

          @Override
          public float flowt() {
            return (float) i;
          }

          @Override
          public double twice() {
            return (double) i;
          }

          @Override
          public Bytes sBytes() {
            return bytes(16);
          }

          @Override
          public Bytes mBytes() {
            return bytes(32);
          }

          @Override
          public Bytes lBytes() {
            return bytes(64);
          }

          private Bytes bytes(int size) {
            Bytes bytes = new Bytes(size);
            ByteBuffer buf = ByteBuffer.wrap(bytes.backingArray());
            for (int pos = 0; pos < size; pos += 4) buf.putInt(i);
            return bytes;
          }
        };

    var handler =
        new EchoConcurrentClient.EchoResponseHandler() {
          @Override
          public boolean onClientDecodeError(long correlationId) {
            return fut.completeExceptionally(new RuntimeException("Client decode error"));
          }

          @Override
          public boolean onResponse(EchoResponseDecode t) {
            if (t.bool() == request.bool() // this will fail half of the requests
                && t.bite() == request.bite()
                && t.sort() == request.sort()
                && t.eent() == request.eent()
                && t.log() == request.log()
                && t.flowt() == request.flowt()
                && t.sBytes().equals(request.sBytes())
                && t.mBytes().equals(request.mBytes())
                && t.lBytes().equals(request.lBytes())) {
              fut.complete(null);
            } else {
              fut.completeExceptionally(
                  new AssertionFailedError("Echo response did not match request"));
            }

            return true;
          }
        };

    while (!client.echo(request, handler)) {
      LockSupport.parkNanos(100_000);
    }

    return fut;
  }

  private static class EchoServerStateMachine implements EchoSingleThreadedStateMachine {
    private final ClaimHandle handle = new ClaimHandle();

    @Override
    public boolean onRequest(
        long clientId, long correlationId, EchoRequestDecode t, EchoSingleThreadedServer server) {
      EchoResponseEncode encode = server.claimEcho(clientId, correlationId, handle);
      if (handle.isFailed()) return false;

      encode.setLBytes(t.lBytes());
      encode.setMBytes(t.mBytes());
      encode.setSBytes(t.sBytes());
      encode.setTwice(t.twice());
      encode.setFlowt(t.flowt());
      encode.setLog(t.log());
      encode.setEent(t.eent());
      encode.setSort(t.sort());
      encode.setBite(t.bite());
      encode.setBool(t.bool());
      handle.commit();

      return true;
    }

    @Override
    public void onClientDisconnected(long clientId) {
      IO.println("on client disconnected " + clientId);
    }

    @Override
    public void onProcessingError(
        long clientId, long correlationId, int messageType, RuntimeException error) {
      IO.println(
          "on processing error "
              + clientId
              + " "
              + correlationId
              + " "
              + messageType
              + " "
              + error);
    }

    @Override
    public void onError(Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  private static class EchoServerAgent implements Agent, AutoCloseable {
    EchoSingleThreadedJarpcServer server;

    EchoServerAgent(Aeron aeron, ConnectionConfig cfg) {
      this.server = EchoSingleThreadedJarpcServer.create(aeron, cfg, new EchoServerStateMachine());
    }

    @Override
    public void close() {
      server.close();
    }

    @Override
    public int doWork() {
      return server.poll(1);
    }

    @Override
    public String roleName() {
      return "";
    }
  }

  private static class ErrorHandler implements ClientErrorHandler {
    @Override
    public void onCallbackNotFound(long correlationId, String type) {
      IO.println("on callback not found " + type);
    }

    @Override
    public void onCorruptPublication(ErrorCode code) {
      IO.println("on corrupt publication " + code);
    }

    @Override
    public void onError(Throwable throwable) {
      throwable.printStackTrace();
    }
  }
}
