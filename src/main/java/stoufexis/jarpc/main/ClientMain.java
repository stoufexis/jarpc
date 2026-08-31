package stoufexis.jarpc.main;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.client.ClientErrorHandler;
import stoufexis.jarpc.exchange.client.ConcurrentExchangeClient;
import stoufexis.jarpc.exchange.client.ConcurrentJarpcExchangeClient;
import stoufexis.jarpc.exchange.common.PostOrderRequestDecode;
import stoufexis.jarpc.exchange.common.PostOrderResponseDecode;
import stoufexis.jarpc.model.ConnectivityConfig;
import stoufexis.jarpc.model.ErrorCode;

public class ClientMain {

  static class ErrorHandlerImpl implements ClientErrorHandler {
    @Override
    public void onCallbackNotFound(long correlationId, String type) {
      System.out.println("CallbackNotFound(" + correlationId + "," + type + ")");
    }

    @Override
    public void onCorruptPublication(ErrorCode code) {
      System.out.println("CorruptPublication(" + code + ")");
    }

    @Override
    public void onError(Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  static void main() {

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
        ConcurrentJarpcExchangeClient client =
            ConcurrentJarpcExchangeClient.create(
                new ConnectivityConfig(
                    aeron,
                    Constants.serverRequest,
                    Constants.requestStreamId,
                    Constants.serverControl,
                    Constants.responseStreamId),
                new ErrorHandlerImpl(),
                1024);
        //
        AgentRunner runner =
            new AgentRunner(new SleepingIdleStrategy(), new ErrorHandlerImpl(), null, client)) {

      AgentRunner.startOnThread(runner);

      while (!client.isConnected()) {
        Thread.sleep(100);
      }

      for (int i = 0; i < 5; i++) {

        final int fi = i;

        boolean posted =
            client.postOrder(
                new PostOrderRequestDecode() {
                  @Override
                  public int getBaseAssetId() {
                    return fi + 1;
                  }

                  @Override
                  public int getQuoteAssetId() {
                    return fi * 10 + 1;
                  }

                  @Override
                  public long getQuantityUnscaled() {
                    return (fi + 1) * 1000;
                  }

                  @Override
                  public int getQuantityScale() {
                    return 0;
                  }

                  @Override
                  public long getRateUnscaled() {
                    return 0;
                  }

                  @Override
                  public int getRateScale() {
                    return 0;
                  }
                },
                new ConcurrentExchangeClient.PostOrderResponseHandler() {
                  @Override
                  public boolean onResponse(PostOrderResponseDecode t) {
                    System.out.println("PostOrderResponse(" + t.getStatusCode() + ")");
                    return true;
                  }

                  @Override
                  public boolean onClientDecodeError(long correlationId) {
                    System.out.println("ClientDecodeError(" + correlationId + ")");
                    return true;
                  }
                });

        System.out.println("Sent: " + posted);
        Thread.sleep(500);
      }

      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
