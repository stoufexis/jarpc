package stoufexis.jarpc.tmp;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;
import stoufexis.jarpc.util.DuplexChannel;

// TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
  static IdleStrategy idle = new SleepingIdleStrategy(100_000_000);
  static ErrorHandler handler = System.out::println;

  static void server() {

    try (DuplexChannel chan =
        DuplexChannel.connect(
            "localhost", "localost", 9001, 9002, 10, ThreadingMode.SHARED, idle); ) {

      Agent subAgent =
          new Agent() {
            private final BufferClaim claim = new BufferClaim();

            @Override
            public int doWork() {
              return chan.subscription()
                  .poll(
                      (buffer, offset, _, _) -> {
                        int i = buffer.getInt(offset);
                        if (chan.publication().tryClaim(4, claim) > 0) {
                          claim.buffer().putInt(claim.offset(), i * i);
                          claim.commit();
                        }
                      },
                      10);
            }

            @Override
            public String roleName() {
              return "sub";
            }
          };

      try (AgentRunner agentRunner = new AgentRunner(idle, handler, null, subAgent)) {
        AgentRunner.startOnThread(agentRunner);
        Thread.sleep(60_000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static void client() {

    try (DuplexChannel chan =
        DuplexChannel.connect(
            "localhost", "localost", 9002, 9001, 10, ThreadingMode.SHARED, idle); ) {

      Agent subAgent =
          new Agent() {
            @Override
            public int doWork() {
              return chan.subscription()
                  .poll(
                      (buffer, offset, _, _) -> {
                        int i = buffer.getInt(offset);
                        System.out.println("Received " + i);
                      },
                      10);
            }

            @Override
            public String roleName() {
              return "sub";
            }
          };

      BufferClaim claim = new BufferClaim();
      try (AgentRunner agentRunner = new AgentRunner(idle, handler, null, subAgent)) {
        AgentRunner.startOnThread(agentRunner);

        for (int i = 0; i < 10; i++) {
          Thread.sleep(2_500);
          if (chan.publication().tryClaim(4, claim) > 0) {
            claim.buffer().putInt(claim.offset(), i);
            claim.commit();
          }
        }

        Thread.sleep(60_000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static void main() {
    if (System.getenv().containsKey("SERVER")) {
      server();
    } else {
      client();
    }
  }
}
