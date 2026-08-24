package stoufexis.jarpc.util;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.concurrent.IdleStrategy;

public final class DuplexChannel implements AutoCloseable {
  private final MediaDriver driver;
  private final Aeron aeron;
  private final Publication publication;
  private final Subscription subscription;

  public DuplexChannel(
      MediaDriver driver, Aeron aeron, Publication publication, Subscription subscription) {
    this.driver = driver;
    this.aeron = aeron;
    this.publication = publication;
    this.subscription = subscription;
  }

  public Publication publication() {
    return publication;
  }

  public Subscription subscription() {
    return subscription;
  }

  public static DuplexChannel connect(
      String localHost,
      String remoteHost,
      int localPort,
      int remotePort,
      int streamId,
      ThreadingMode mediaDriverThreading,
      IdleStrategy mediaDriverIdleStrategy) {
    MediaDriver driver = null;
    Aeron aeron = null;
    Publication publication = null;
    Subscription subscription = null;

    try {
      MediaDriver.Context mediaDriverCtx =
          new MediaDriver.Context()
              .dirDeleteOnStart(true)
              .threadingMode(mediaDriverThreading)
              .sharedIdleStrategy(mediaDriverIdleStrategy)
              .dirDeleteOnShutdown(true);

      driver = MediaDriver.launchEmbedded(mediaDriverCtx);

      // construct Aeron, pointing at the media driver's folder
      Aeron.Context aeronCtx = new Aeron.Context().aeronDirectoryName(driver.aeronDirectoryName());

      aeron = Aeron.connect(aeronCtx);

      // construct the subs and pubs
      subscription = aeron.addSubscription(udpChannel(localHost, localPort), streamId);
      publication = aeron.addPublication(udpChannel(remoteHost, remotePort), streamId);

      return new DuplexChannel(driver, aeron, publication, subscription);

    } catch (RuntimeException e) {
      CloseHelper.closeAll(subscription, publication, aeron, driver);
      throw new RuntimeException(e);
    }
  }

  private static String udpChannel(String host, int port) {
    return "aeron:udp?endpoint=" + host + ":" + port;
  }

  @Override
  public void close() {
    CloseHelper.closeAll(subscription, publication, aeron, driver);
  }
}
