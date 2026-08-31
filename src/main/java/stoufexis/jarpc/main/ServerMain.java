package stoufexis.jarpc.main;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import stoufexis.jarpc.exchange.common.CancelAllRequestDecode;
import stoufexis.jarpc.exchange.common.PostOrderRequestDecode;
import stoufexis.jarpc.exchange.server.ConcurrentExchangeServer;
import stoufexis.jarpc.exchange.server.ConcurrentJarpcExchangeServer;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.model.ConnectivityConfig;
import stoufexis.jarpc.server.ServerErrorHandler;

public class ServerMain {

  static class ExchangeServerImpl implements ConcurrentExchangeServer {
    private volatile PostOrderResponseHandler postOrderResponse;
    private volatile CancelAllResponseHandler cancelAllResponse;

    @Override
    public void registerHandlers(
        PostOrderResponseHandler postOrderResponse, CancelAllResponseHandler cancelAllResponse) {
      this.postOrderResponse = postOrderResponse;
      this.cancelAllResponse = cancelAllResponse;
    }

    @Override
    public boolean postOrder(long clientId, long correlationId, PostOrderRequestDecode request) {
      System.out.println(
          "PostOrderRequest("
              + request.getBaseAssetId()
              + ","
              + request.getQuoteAssetId()
              + ","
              + request.getQuantityUnscaled()
              + ")");
      return postOrderResponse.onResponse(clientId, correlationId, request::getBaseAssetId);
    }

    @Override
    public boolean cancelAll(long clientId, long correlationId, CancelAllRequestDecode request) {
      return cancelAllResponse.onResponse(clientId, correlationId, () -> 1);
    }
  }

  static class ErrorHandlerImpl implements ServerErrorHandler {
    @Override
    public void onInternalError(long clientId, long correlationId, int errorCode) {
      System.out.println(
          "InternalError(" + clientId + ", " + correlationId + "," + errorCode + ")");
    }

    @Override
    public void onProcessingError(long clientId, long correlationId, int messageType) {
      System.out.println(
          "ProcessingError(" + clientId + ", " + correlationId + "," + messageType + ")");
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
        ConcurrentJarpcExchangeServer server =
            ConcurrentJarpcExchangeServer.create(
                new ExchangeServerImpl(),
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
            new AgentRunner(new SleepingIdleStrategy(), new ErrorHandlerImpl(), null, server)) {

      runner.run();
    }
  }
}
