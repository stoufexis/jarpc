package stoufexis.sample;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.AgentRunner;

public final class ServerMain {

  static void main() {
    try (MediaDriver mediaDriver = Shared.mediaDriver();
        Aeron aeron = Shared.aeron(mediaDriver);
        LeaseServer server = new LeaseServer(aeron, Shared.connection);
        AgentRunner agentRunner = Shared.runner(server)) {
      agentRunner.run();
    }
  }
}
