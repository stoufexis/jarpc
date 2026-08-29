package stoufexis.jarpc.main;

// import io.aeron.Aeron;
// import io.aeron.driver.MediaDriver;
// import io.aeron.driver.ThreadingMode;
// import org.agrona.ErrorHandler;
// import org.agrona.concurrent.AgentRunner;
// import org.agrona.concurrent.SleepingIdleStrategy;

public class ClientMain {

  static void main() {
    //    ErrorHandler errorHandler = throwable -> throwable.printStackTrace();
    //
    //    try (MediaDriver mediaDriver =
    //            MediaDriver.launchEmbedded(
    //                new MediaDriver.Context()
    //                    .dirDeleteOnStart(true)
    //                    .threadingMode(ThreadingMode.SHARED)
    //                    .sharedIdleStrategy(new SleepingIdleStrategy())
    //                    .dirDeleteOnShutdown(true));
    //        //
    //        Aeron aeron =
    //            Aeron.connect(
    //                new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));
    //        //
    //        JarpcExchangeClient client =
    //            JarpcExchangeClient.create(
    //                100,
    //                aeron,
    //                Constants.serverRequest,
    //                Constants.requestStreamId,
    //                Constants.serverControl,
    //                Constants.responseStreamId,
    //                errorHandler);
    //        //
    //        AgentRunner runner =
    //            new AgentRunner(new SleepingIdleStrategy(), errorHandler, null, client)) {
    //
    //      AgentRunner.startOnThread(runner);
    //
    //      while (!client.isConnected()) {
    //        Thread.sleep(100);
    //      }
    //
    //      PostOrderRequest request = new PostOrderRequest();
    //
    //      for (int i = 0; i < 5; i++) {
    //
    //        request.set(1 + i, 2 + i, 1 + i, 0, 100 + i, 0);
    //        System.out.println("Sending " + request.toString());
    //        int code =
    //            client.postOrder(
    //                request,
    //                new ExchangeClient.PostOrderCallback() {
    //                  @Override
    //                  public boolean onResponse(boolean last, int correlationId, PostOrderResponse
    // t) {
    //                    System.out.println("Received " + t.toString());
    //                    return true;
    //                  }
    //
    //                  @Override
    //                  public boolean onClientDecodeError(
    //                      int correlationId, RuntimeException exception) {
    //                    System.out.println(exception);
    //                    return true;
    //                  }
    //
    //                  @Override
    //                  public boolean onServerDecodeError(int correlationId) {
    //                    System.out.println("Server decode error");
    //                    return true;
    //                  }
    //                });
    //
    //        System.out.println("Sent " + code);
    //        Thread.sleep(500);
    //      }
    //
    //      Thread.sleep(5000);
    //    } catch (InterruptedException e) {
    //      throw new RuntimeException(e);
    //    }
  }
}
