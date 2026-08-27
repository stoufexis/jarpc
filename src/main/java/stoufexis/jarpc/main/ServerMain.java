package stoufexis.jarpc.main;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.exchange.model.CancelAllRequest;
import stoufexis.jarpc.exchange.model.CancelAllResponse;
import stoufexis.jarpc.exchange.model.PostOrderRequest;
import stoufexis.jarpc.exchange.model.PostOrderResponse;
import stoufexis.jarpc.exchange.server.ExchangeServer;
import stoufexis.jarpc.exchange.server.JarpcExchangeServer;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.server.ServerErrorHandler;

public class ServerMain {
  static class ExchangeServerImpl implements ExchangeServer {
    @Override
    public boolean postOrder(
        long clientId, long correlationId, PostOrderRequest request, PostOrderCallback callback) {
      System.out.println("Received " + request.toString());
      PostOrderResponse response = new PostOrderResponse();
      response.set(1);
      System.out.println("Sending " + response.toString());
      ErrorCode code = callback.onResponse(clientId, correlationId, true, response);
      System.out.println("Sent " + code);
      return code == null;
    }

    @Override
    public boolean cancelAll(
        long clientId, long correlationId, CancelAllRequest request, CancelAllCallback callback) {
      System.out.println("Received " + request.toString());
      CancelAllResponse response = new CancelAllResponse();
      response.set(1);
      System.out.println("Sending " + response.toString());
      ErrorCode code = callback.onResponse(clientId, correlationId, true, response);
      System.out.println("Sent " + code);
      return code == null;
    }
  }

  record InternalError(long clientId, long correlationId, ErrorCode code) {}

  static void main() {

    ServerErrorHandler errorHandler =
        new ServerErrorHandler() {
          @Override
          public void onInternalError(long clientId, long correlationId, ErrorCode code) {
            System.out.println(new InternalError(clientId, correlationId, code));
          }

          @Override
          public void onError(Throwable throwable) {
            throwable.printStackTrace();
          }
        };

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
        JarpcExchangeServer server =
            JarpcExchangeServer.create(
                aeron,
                new ExchangeServerImpl(),
                errorHandler,
                Constants.serverRequest,
                Constants.requestStreamId,
                Constants.serverControl,
                Constants.responseStreamId);
        //
        AgentRunner runner =
            new AgentRunner(new SleepingIdleStrategy(), errorHandler, null, server)) {

      runner.run();
    }
  }
}
