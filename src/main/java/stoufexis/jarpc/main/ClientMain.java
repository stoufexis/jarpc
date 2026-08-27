package stoufexis.jarpc.main;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.exchange.client.ExchangeClient;
import stoufexis.jarpc.exchange.client.JarpcExchangeClient;
import stoufexis.jarpc.exchange.model.PostOrderRequest;
import stoufexis.jarpc.exchange.model.PostOrderResponse;

public class ClientMain {

  static void main() {
    ErrorHandler errorHandler = throwable -> throwable.printStackTrace();

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
        JarpcExchangeClient client =
            JarpcExchangeClient.create(
                aeron,
                Constants.serverRequest,
                Constants.requestStreamId,
                Constants.serverControl,
                Constants.responseStreamId,
                errorHandler);
        //
        AgentRunner runner =
            new AgentRunner(new SleepingIdleStrategy(), errorHandler, null, client)) {

      AgentRunner.startOnThread(runner);

      PostOrderRequest request = new PostOrderRequest();
      request.set(1, 2, 1, 0, 100, 0);
      System.out.println("Sending " + request.toString());
      client.postOrder(
          1,
          request,
          new ExchangeClient.PostOrderCallback() {
            @Override
            public boolean onResponse(long correlationId, PostOrderResponse t) {
              System.out.println("Received " + t.toString());
              return true;
            }

            @Override
            public void onClientDecodeError(long correlationId, RuntimeException exception) {
              System.out.println(exception);
            }

            @Override
            public void onServerDecodeError(long correlationId) {
              System.out.println("Server decode error");
            }
          });

      Thread.sleep(2500);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
