package stoufexis.sample;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.lib.model.ConnectivityConfig;

public final class ServerMain {
  static void main() {

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

      try (LeaseServer server = new LeaseServer(cfg);
          AgentRunner agentRunner =
              new AgentRunner(new SleepingIdleStrategy(), System.out::println, null, server)) {
        agentRunner.run();
      }
    }
  }
}
