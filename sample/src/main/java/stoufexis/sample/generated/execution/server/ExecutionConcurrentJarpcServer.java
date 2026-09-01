package stoufexis.sample.generated.execution.server;

import io.aeron.Subscription;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;

import stoufexis.sample.generated.execution.common.*;

public final class ExecutionConcurrentJarpcServer implements Agent, AutoCloseable {

  private final ExecutionSingleThreadedJarpcServer singleThreadedServer;

  private final ExecutionConcurrentServer concurrentExecutionServer;

  private final ServerErrorHandler errorHandler;


  private final ExecutionConcurrentServer.PostLimitOrderResponseHandler postLimitOrderResponseHandler;
  private final MPSCRingBuffer<PostLimitOrderResponseScratch> postLimitOrderResponses;
  private final PostLimitOrderAgent postLimitOrderAgent;
  private final ExecutionConcurrentServer.CancelOrderResponseHandler cancelOrderResponseHandler;
  private final MPSCRingBuffer<CancelOrderResponseScratch> cancelOrderResponses;
  private final CancelOrderAgent cancelOrderAgent;


  ExecutionConcurrentJarpcServer(
      ExecutionConcurrentServer concurrentExecutionServer,
      Subscription subscription,
      ServerPublications publications,
      ServerErrorHandler errorHandler,
      Images images,
      int queueCapacity) {
    this.errorHandler = errorHandler;
    this.concurrentExecutionServer = concurrentExecutionServer;
    this.singleThreadedServer =
        new ExecutionSingleThreadedJarpcServer(
            subscription,
            publications,
            images,

            new PostLimitOrderRequestHandler(),
            new CancelOrderRequestHandler(),

            concurrentExecutionServer,
            errorHandler);

    this.postLimitOrderResponseHandler = new PostLimitOrderResponseHandler();
    this.postLimitOrderResponses = new MPSCRingBuffer<>(queueCapacity, PostLimitOrderResponseScratch::new);
    this.postLimitOrderAgent = new PostLimitOrderAgent();
    this.cancelOrderResponseHandler = new CancelOrderResponseHandler();
    this.cancelOrderResponses = new MPSCRingBuffer<>(queueCapacity, CancelOrderResponseScratch::new);
    this.cancelOrderAgent = new CancelOrderAgent();

  }

  public static ExecutionConcurrentJarpcServer create(
      ExecutionConcurrentServer concurrentExecutionServer,
      ConnectivityConfig cfg,
      ServerErrorHandler errorHandler,
      int queueCapacity) {
    Images images = new Images();

    Subscription subscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new ExecutionConcurrentJarpcServer(
        concurrentExecutionServer,
        subscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        errorHandler,
        images,
        queueCapacity);
  }

  @Override
  public void onStart() {

    concurrentExecutionServer.registerPostLimitOrder(postLimitOrderResponseHandler);
    concurrentExecutionServer.registerCancelOrder(cancelOrderResponseHandler);

  }

  @Override
  public int doWork() {
    int work = 0;
    work += singleThreadedServer.poll(1);

    work += postLimitOrderAgent.doWork();
    work += cancelOrderAgent.doWork();

    return work;
  }

  @Override
  public String roleName() {
    return "ConcurrentJarpcExecutionServer";
  }

  @Override
  public void close() {
    singleThreadedServer.close();
  }


  private final class PostLimitOrderRequestHandler
      implements ExecutionSingleThreadedServer.PostLimitOrderRequestHandler {
    @Override
    public boolean onRequest(long clientId, long correlationId, PostLimitOrderRequestDecode t) {
      return concurrentExecutionServer.postLimitOrder(clientId, correlationId, t);
    }
  }

  private final class PostLimitOrderResponseHandler
      implements ExecutionConcurrentServer.PostLimitOrderResponseHandler {
    @Override
    public boolean onResponse(long clientId, long correlationId, PostLimitOrderResponseDecode t) {
      return postLimitOrderResponses.offer(PostLimitOrderResponseScratch::setter, t, clientId, correlationId);
    }
  }

  private final class PostLimitOrderAgent extends MPSCBufferPollAgent<PostLimitOrderResponseScratch> {
    PostLimitOrderAgent() {
      super(
          postLimitOrderResponses, new PostLimitOrderResponseScratch(), PostLimitOrderResponseScratch::copy, errorHandler);
    }

    @Override
    protected boolean processRequest(PostLimitOrderResponseScratch scratch) {
      PostLimitOrderResponseEncode encode =
          singleThreadedServer.claimPostLimitOrder(
              scratch.getClientId(), scratch.getCorrelationId(), claimHandle);

      if (encode == null) return handleError();

      encode.set(scratch);
      return true;
    }
  }
  private final class CancelOrderRequestHandler
      implements ExecutionSingleThreadedServer.CancelOrderRequestHandler {
    @Override
    public boolean onRequest(long clientId, long correlationId, CancelOrderRequestDecode t) {
      return concurrentExecutionServer.cancelOrder(clientId, correlationId, t);
    }
  }

  private final class CancelOrderResponseHandler
      implements ExecutionConcurrentServer.CancelOrderResponseHandler {
    @Override
    public boolean onResponse(long clientId, long correlationId, CancelOrderResponseDecode t) {
      return cancelOrderResponses.offer(CancelOrderResponseScratch::setter, t, clientId, correlationId);
    }
  }

  private final class CancelOrderAgent extends MPSCBufferPollAgent<CancelOrderResponseScratch> {
    CancelOrderAgent() {
      super(
          cancelOrderResponses, new CancelOrderResponseScratch(), CancelOrderResponseScratch::copy, errorHandler);
    }

    @Override
    protected boolean processRequest(CancelOrderResponseScratch scratch) {
      CancelOrderResponseEncode encode =
          singleThreadedServer.claimCancelOrder(
              scratch.getClientId(), scratch.getCorrelationId(), claimHandle);

      if (encode == null) return handleError();

      encode.set(scratch);
      return true;
    }
  }

}

