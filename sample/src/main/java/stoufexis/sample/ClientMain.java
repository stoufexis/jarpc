package stoufexis.sample;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.model.Bytes;
import stoufexis.jarpc.lib.model.ConnectivityConfig;
import stoufexis.jarpc.lib.model.ErrorCode;
import stoufexis.sample.generated.lease.client.LeaseConcurrentJarpcClient;
import stoufexis.sample.generated.lease.common.AcquireRequestDecode;
import stoufexis.sample.generated.lease.common.QueryRequestDecode;
import stoufexis.sample.generated.lease.common.RefreshRequestDecode;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class ClientMain {
  static class ErrorHandler implements ClientErrorHandler {
    @Override
    public void onCallbackNotFound(long correlationId, String type) {
      System.out.println("Callback not found " + type);
    }

    @Override
    public void onCorruptPublication(ErrorCode code) {
      System.out.println("Corrupt publication " + code);
    }

    @Override
    public void onError(Throwable throwable) {
      System.out.println(throwable.toString());
    }
  }

  record Acquire(long key, Bytes value) implements AcquireRequestDecode {}

  record Query(long key) implements QueryRequestDecode {}

  record Refresh(long key) implements RefreshRequestDecode {}

  static void main() throws InterruptedException {
    try (MediaDriver mediaDriver =
            MediaDriver.launchEmbedded(
                new MediaDriver.Context()
                    .dirDeleteOnStart(true)
                    .threadingMode(ThreadingMode.SHARED)
                    .sharedIdleStrategy(new SleepingIdleStrategy())
                    .dirDeleteOnShutdown(true));
        Aeron aeron =
            Aeron.connect(
                new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()))) {

      ConnectivityConfig cfg = Constants.connectivityConfig(aeron);

      try (LeaseConcurrentJarpcClient client =
              LeaseConcurrentJarpcClient.create(cfg, new ErrorHandler(), 1024);
          AgentRunner agentRunner =
              new AgentRunner(new SleepingIdleStrategy(), System.out::println, null, client)) {

        AgentRunner.startOnThread(agentRunner);

        while (!client.isConnected()) {
          Thread.sleep(100);
        }
      }
    }
  }
}
