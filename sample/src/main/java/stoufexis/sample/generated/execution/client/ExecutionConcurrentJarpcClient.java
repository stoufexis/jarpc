package stoufexis.sample.generated.execution.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.util.*;

import stoufexis.sample.generated.execution.common.*;

import static stoufexis.jarpc.lib.util.Util.createClientSubscription;
import static stoufexis.jarpc.lib.util.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancel

public final class ExecutionConcurrentJarpcClient
    implements ExecutionConcurrentClient, Agent, AutoCloseable {

  private final ExecutionSingleThreadedJarpcClient singleThreadedClient;


  private final MPSCRingBuffer<PostLimitOrderRequestScratch> postLimitOrderRequests;

  private final Long2ObjectHashMap<PostLimitOrderResponseHandler> postLimitOrderCallbacks;
  private final PostLimitOrderAgent postLimitOrderAgent;

  private final MPSCRingBuffer<CancelOrderRequestScratch> cancelOrderRequests;

  private final Long2ObjectHashMap<CancelOrderResponseHandler> cancelOrderCallbacks;
  private final CancelOrderAgent cancelOrderAgent;



  private final ClientErrorHandler errorHandler;

  private final ConnectivityProbe connectivityProbe;

  ExecutionConcurrentJarpcClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {

    this.postLimitOrderCallbacks = new Long2ObjectHashMap<>();
    this.postLimitOrderRequests = new MPSCRingBuffer<>(queueCapacity, PostLimitOrderRequestScratch::new);
    this.postLimitOrderAgent = new PostLimitOrderAgent();
    this.cancelOrderCallbacks = new Long2ObjectHashMap<>();
    this.cancelOrderRequests = new MPSCRingBuffer<>(queueCapacity, CancelOrderRequestScratch::new);
    this.cancelOrderAgent = new CancelOrderAgent();

    this.singleThreadedClient =
        new ExecutionSingleThreadedJarpcClient(
            publication,
            subscription,

            new PostLimitOrderHandler(),
            new CancelOrderHandler(),

            errorHandler);
    this.errorHandler = errorHandler;
    this.connectivityProbe = new ConnectivityProbe(singleThreadedClient);
  }

  public static ExecutionConcurrentJarpcClient create(
      ConnectivityConfig cfg, ClientErrorHandler handler, int queueCapacity) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new ExecutionConcurrentJarpcClient(pub, sub, handler, queueCapacity);
  }


  @Override
  public boolean postLimitOrder(PostLimitOrderRequestDecode request, PostLimitOrderResponseHandler response) {
    return postLimitOrderRequests.offer(PostLimitOrderRequestScratch::setter, request, response);
  }

  @Override
  public boolean cancelOrder(CancelOrderRequestDecode request, CancelOrderResponseHandler response) {
    return cancelOrderRequests.offer(CancelOrderRequestScratch::setter, request, response);
  }



  @Override
  public int doWork() {
    int work = 0;

    connectivityProbe.probeConnected();

    // Use this instead of CompositeAgent to monomorphize all calls to doWork

    work += postLimitOrderAgent.doWork();
    work += cancelOrderAgent.doWork();

    work += singleThreadedClient.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "ExecutionConcurrentJarpcClient";
  }

  @Override
  public void close() {
    singleThreadedClient.close();
  }

  public boolean isConnected() {
    return connectivityProbe.isConnected();
  }


  private final class PostLimitOrderHandler extends ResponseHandlerUtil<PostLimitOrderResponseHandler>
      implements ExecutionSingleThreadedJarpcClient.PostLimitOrderResponseHandler {
    PostLimitOrderHandler() {
      super(postLimitOrderCallbacks, errorHandler, "PostLimitOrder");
    }

    @Override
    public boolean onResponse(long correlationId, PostLimitOrderResponseDecode t) {
      PostLimitOrderResponseHandler callback = getCallback(correlationId);

      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);

      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class PostLimitOrderAgent extends MPSCBufferPollAgent<PostLimitOrderRequestScratch> {
    PostLimitOrderAgent() {
      super(postLimitOrderRequests, new PostLimitOrderRequestScratch(), PostLimitOrderRequestScratch::copy, errorHandler);
    }

    @Override
    protected boolean processRequest(PostLimitOrderRequestScratch scratch) {
      PostLimitOrderRequestEncode encode = singleThreadedClient.claimPostLimitOrder(claimHandle);

      if (encode == null) return handleError();

      postLimitOrderCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }
  private final class CancelOrderHandler extends ResponseHandlerUtil<CancelOrderResponseHandler>
      implements ExecutionSingleThreadedJarpcClient.CancelOrderResponseHandler {
    CancelOrderHandler() {
      super(cancelOrderCallbacks, errorHandler, "CancelOrder");
    }

    @Override
    public boolean onResponse(long correlationId, CancelOrderResponseDecode t) {
      CancelOrderResponseHandler callback = getCallback(correlationId);

      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);

      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class CancelOrderAgent extends MPSCBufferPollAgent<CancelOrderRequestScratch> {
    CancelOrderAgent() {
      super(cancelOrderRequests, new CancelOrderRequestScratch(), CancelOrderRequestScratch::copy, errorHandler);
    }

    @Override
    protected boolean processRequest(CancelOrderRequestScratch scratch) {
      CancelOrderRequestEncode encode = singleThreadedClient.claimCancelOrder(claimHandle);

      if (encode == null) return handleError();

      cancelOrderCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }

}

