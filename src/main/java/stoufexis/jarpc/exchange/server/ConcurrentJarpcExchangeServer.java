package stoufexis.jarpc.exchange.server;

import io.aeron.Subscription;
import org.agrona.concurrent.Agent;
import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.server.Images;
import stoufexis.jarpc.server.ServerConfig;
import stoufexis.jarpc.server.ServerErrorHandler;
import stoufexis.jarpc.server.ServerPublications;
import stoufexis.jarpc.util.MPSCBufferPollAgent;
import stoufexis.jarpc.util.MPSCRingBuffer;

import static stoufexis.jarpc.util.Util.createServerSubscription;

public final class ConcurrentJarpcExchangeServer implements Agent {

  private final SingleThreadedExchangeServer singleThreadedServer;

  private final ConcurrentExchangeServer concurrentExchangeServer;

  private final ServerErrorHandler errorHandler;

  private final ConcurrentExchangeServer.PostOrderResponseHandler postOrderResponseHandler;
  private final ConcurrentExchangeServer.CancelAllResponseHandler cancelAllRequestHandler;

  private final MPSCRingBuffer<PostOrderResponseScratch> postOrderResponses;
  private final MPSCRingBuffer<CancelAllResponseScratch> cancelAllResponses;

  private final PostOrderAgent postOrderAgent;
  private final CancelAllAgent cancelAllAgent;

  ConcurrentJarpcExchangeServer(
      ConcurrentExchangeServer concurrentExchangeServer,
      Subscription subscription,
      ServerPublications publications,
      ServerErrorHandler errorHandler,
      Images images,
      int queueCapacity) {
    this.errorHandler = errorHandler;
    this.concurrentExchangeServer = concurrentExchangeServer;
    this.singleThreadedServer =
        new SingleThreadedJarpcExchangeServer(
            subscription,
            publications,
            images,
            errorHandler,
            new PostOrderRequestHandler(),
            new CancelAllRequestHandler());
    this.postOrderResponseHandler = new PostOrderResponseHandler();
    this.cancelAllRequestHandler = new CancelAllResponseHandler();
    this.postOrderResponses = new MPSCRingBuffer<>(queueCapacity, PostOrderResponseScratch::new);
    this.cancelAllResponses = new MPSCRingBuffer<>(queueCapacity, CancelAllResponseScratch::new);
    this.postOrderAgent = new PostOrderAgent();
    this.cancelAllAgent = new CancelAllAgent();
  }

  public static ConcurrentJarpcExchangeServer create(
      ConcurrentExchangeServer concurrentExchangeServer,
      ServerConfig cfg,
      ServerErrorHandler errorHandler,
      int queueCapacity) {
    Images images = new Images();

    Subscription subscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new ConcurrentJarpcExchangeServer(
        concurrentExchangeServer,
        subscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        errorHandler,
        images,
        queueCapacity);
  }

  @Override
  public void onStart() {
    concurrentExchangeServer.registerHandlers(postOrderResponseHandler, cancelAllRequestHandler);
  }

  @Override
  public int doWork() {
    int work = 0;
    work += singleThreadedServer.poll(1);
    work += postOrderAgent.doWork();
    work += cancelAllAgent.doWork();
    return work;
  }

  @Override
  public String roleName() {
    return "ConcurrentJarpcExchangeServer";
  }

  private final class PostOrderRequestHandler
      implements SingleThreadedExchangeServer.PostOrderRequestHandler {
    @Override
    public boolean onRequest(long clientId, long correlationId, PostOrderRequestDecode t) {
      return concurrentExchangeServer.postOrder(clientId, correlationId, t);
    }
  }

  private final class CancelAllRequestHandler
      implements SingleThreadedExchangeServer.CancelAllRequestHandler {
    @Override
    public boolean onRequest(long clientId, long correlationId, CancelAllRequestDecode t) {
      return concurrentExchangeServer.cancelAll(clientId, correlationId, t);
    }
  }

  private final class PostOrderResponseHandler
      implements ConcurrentExchangeServer.PostOrderResponseHandler {
    @Override
    public boolean onResponse(long clientId, long correlationId, PostOrderResponseDecode t) {
      return postOrderResponses.offer(PostOrderResponseScratch::setter, t, clientId, correlationId);
    }
  }

  private final class CancelAllResponseHandler
      implements ConcurrentExchangeServer.CancelAllResponseHandler {
    @Override
    public boolean onResponse(long clientId, long correlationId, CancelAllResponseDecode t) {
      return cancelAllResponses.offer(CancelAllResponseScratch::setter, t, clientId, correlationId);
    }
  }

  private final class PostOrderAgent extends MPSCBufferPollAgent<PostOrderResponseScratch> {
    PostOrderAgent() {
      super(
          postOrderResponses,
          new PostOrderResponseScratch(),
          PostOrderResponseScratch::copy,
          errorHandler::onCorruptPublication);
    }

    @Override
    protected boolean processRequest(PostOrderResponseScratch scratch) {
      PostOrderResponseEncode encode =
          singleThreadedServer.claimPostOrder(
              scratch.getClientId(), scratch.getCorrelationId(), claimHandle);

      if (encode == null) return handleError();

      encode.set(scratch);
      return true;
    }
  }

  private final class CancelAllAgent extends MPSCBufferPollAgent<CancelAllResponseScratch> {
    CancelAllAgent() {
      super(
          cancelAllResponses,
          new CancelAllResponseScratch(),
          CancelAllResponseScratch::copy,
          errorHandler::onCorruptPublication);
    }

    @Override
    protected boolean processRequest(CancelAllResponseScratch scratch) {
      CancelAllResponseEncode encode =
          singleThreadedServer.claimCancelAll(
              scratch.getClientId(), scratch.getCorrelationId(), claimHandle);

      if (encode == null) return handleError();

      encode.set(scratch);
      return true;
    }
  }
}
