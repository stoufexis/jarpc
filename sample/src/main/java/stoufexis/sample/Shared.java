package stoufexis.sample;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.lib.model.ConnectivityConfig;

public final class Shared {
  public static ConnectivityConfig connectivityConfig(Aeron aeron) {
    return new ConnectivityConfig(aeron, "localhost:10000", 1, "localhost:10001", 2);
  }

  public static MediaDriver mediaDriver() {
    return MediaDriver.launchEmbedded(
        new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new SleepingIdleStrategy())
            .dirDeleteOnShutdown(true));
  }

  public static Aeron aeron(MediaDriver mediaDriver) {
    return Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));
  }

  public static AgentRunner runner(Agent agent) {
    return new AgentRunner(new SleepingIdleStrategy(), System.out::println, null, agent);
  }
}
