package stoufexis.jarpc.tmp;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import org.agrona.ErrorHandler;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;

// TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

  static class Foo {
    int bar;
    double baz;

    @Override
    public String toString() {
      return "Foo{" + "bar=" + bar + ", baz=" + baz + '}';
    }
  }

  static Foo buff = new Foo();
  static Decoder<Foo> decoder =
      (offset, length, b) -> {
        buff.bar = b.getInt(offset);
        buff.baz = b.getDouble(offset + 4);
        return buff;
      };

  static Encoder<Foo> encoder =
      new Encoder<>() {
        @Override
        public int length(Foo foo) {
          return 12;
        }

        @Override
        public void encode(int offset, Foo foo, MutableDirectBuffer b) {
          b.putInt(offset, foo.bar);
          b.putDouble(offset + 4, foo.baz);
        }
      };

  public static final String ENDPOINT = "localhost:40123";

  private static IdleStrategy idle = new SleepingIdleStrategy(250_000_000);
  private static ErrorHandler err = throwable -> System.out.println("Error " + throwable);

  static void publisher(Aeron aeron) {
    try (Publication pub = aeron.addPublication("aeron:udp?endpoint=" + ENDPOINT, 1)) {
      var qPub = new QueuedPublisher<>(encoder, pub, 4096);

      var senderAgent =
          new Agent() {
            int cnt;
            Foo buf = new Foo();

            @Override
            public int doWork() {
              buf.bar = cnt;
              buf.baz = 1.123 + cnt;
              qPub.enqueue(buf);
              cnt++;
              return 1;
            }

            @Override
            public String roleName() {
              return "";
            }
          };

      var composite = new CompositeAgent(senderAgent, qPub.agent());

      try (AgentRunner runner = new AgentRunner(idle, err, null, composite)) {
        AgentRunner.startOnThread(runner);
        Thread.sleep(10_000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static void subscriber(Aeron aeron) {
    try (Subscription sub = aeron.addSubscription("aeron:udp?endpoint=" + ENDPOINT, 1)) {
      var qSub =
          new QueuedSubscriber<>(
              decoder,
              sub,
              4096,
              elem -> {
                System.out.println(elem);
                return true;
              });

      var consumerAgent =
          new Agent() {
            @Override
            public int doWork() throws Exception {
              qSub.poll();
              return 1;
            }

            @Override
            public String roleName() {
              return "";
            }
          };

      var composite = new CompositeAgent(qSub.agent(), consumerAgent);

      try (AgentRunner runner = new AgentRunner(idle, err, null, composite)) {
        AgentRunner.startOnThread(runner);
        Thread.sleep(20_000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static void main() {

    try (MediaDriver driver = MediaDriver.launchEmbedded();
        Aeron aeron =
            Aeron.connect(new Aeron.Context().aeronDirectoryName(driver.aeronDirectoryName()))) {

      if (System.getenv("SIDE").equals("PUB")) {
        publisher(aeron);
      } else {
        subscriber(aeron);
      }
    }
  }
}
