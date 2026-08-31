package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.client.ConnectivityProbe;
import stoufexis.jarpc.model.ConnectivityConfig;
import stoufexis.jarpc.util.MPSCBufferPollAgent;
import stoufexis.jarpc.client.ClientErrorHandler;
import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.util.MPSCRingBuffer;
import stoufexis.jarpc.util.ResponseHandlerUtil;

import static stoufexis.jarpc.util.Util.createClientSubscription;
import static stoufexis.jarpc.util.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancel

public final class ConcurrentJarpcExchangeClient
    implements ConcurrentExchangeClient, Agent, AutoCloseable {

  private final SingleThreadedJarpcExchangeClient singleThreadedClient;

  private final MPSCRingBuffer<PostOrderRequestScratch> postOrderRequests;
  private final MPSCRingBuffer<CancelAllRequestScratch> cancelAllRequests;

  private final ClientErrorHandler errorHandler;

  private final Long2ObjectHashMap<PostOrderResponseHandler> postOrderCallbacks;
  private final Long2ObjectHashMap<CancelAllResponseHandler> cancelAllCallbacks;

  private final PostOrderAgent postOrderAgent;
  private final CancelAllAgent cancelAllAgent;

  private final ConnectivityProbe connectivityProbe;

  ConcurrentJarpcExchangeClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {
    this.postOrderCallbacks = new Long2ObjectHashMap<>();
    this.cancelAllCallbacks = new Long2ObjectHashMap<>();
    this.singleThreadedClient =
        new SingleThreadedJarpcExchangeClient(
            publication,
            subscription,
            new PostOrderHandler(),
            new CancelAllHandler(),
            errorHandler);
    this.postOrderRequests = new MPSCRingBuffer<>(queueCapacity, PostOrderRequestScratch::new);
    this.cancelAllRequests = new MPSCRingBuffer<>(queueCapacity, CancelAllRequestScratch::new);
    this.errorHandler = errorHandler;
    this.postOrderAgent = new PostOrderAgent();
    this.cancelAllAgent = new CancelAllAgent();
    this.connectivityProbe = new ConnectivityProbe(singleThreadedClient);
  }

  public static ConcurrentJarpcExchangeClient create(
      ConnectivityConfig cfg, ClientErrorHandler handler, int queueCapacity) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new ConcurrentJarpcExchangeClient(pub, sub, handler, queueCapacity);
  }

  @Override
  public boolean postOrder(PostOrderRequestDecode request, PostOrderResponseHandler response) {
    return postOrderRequests.offer(PostOrderRequestScratch::setter, request, response);
  }

  @Override
  public boolean cancelAll(CancelAllRequestDecode request, CancelAllResponseHandler response) {
    return cancelAllRequests.offer(CancelAllRequestScratch::setter, request, response);
  }

  @Override
  public int doWork() {
    int work = 0;

    connectivityProbe.probeConnected();

    // Use this instead of CompositeAgent to monomorphize all calls to doWork
    // FIXME verify rationale
    work += postOrderAgent.doWork();
    work += cancelAllAgent.doWork();
    work += singleThreadedClient.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "ConcurrentJarpcExchangeClient";
  }

  @Override
  public void close() {
    singleThreadedClient.close();
  }

  public boolean isConnected() {
    return connectivityProbe.isConnected();
  }

  private final class PostOrderHandler extends ResponseHandlerUtil<PostOrderResponseHandler>
      implements SingleThreadedJarpcExchangeClient.PostOrderResponseHandler {
    PostOrderHandler() {
      super(postOrderCallbacks, errorHandler, "PostOrder");
    }

    @Override
    public boolean onResponse(long correlationId, PostOrderResponseDecode t) {
      PostOrderResponseHandler callback = getCallback(correlationId);

      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);

      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class CancelAllHandler extends ResponseHandlerUtil<CancelAllResponseHandler>
      implements SingleThreadedJarpcExchangeClient.CancelAllResponseHandler {
    CancelAllHandler() {
      super(cancelAllCallbacks, errorHandler, "CancelAll");
    }

    @Override
    public boolean onResponse(long correlationId, CancelAllResponseDecode t) {
      CancelAllResponseHandler callback = getCallback(correlationId);

      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);

      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class PostOrderAgent extends MPSCBufferPollAgent<PostOrderRequestScratch> {
    PostOrderAgent() {
      super(
          postOrderRequests,
          new PostOrderRequestScratch(),
          PostOrderRequestScratch::copy,
          errorHandler::onCorruptPublication);
    }

    @Override
    protected boolean processRequest(PostOrderRequestScratch scratch) {
      PostOrderRequestEncode encode = singleThreadedClient.claimPostOrder(claimHandle);

      if (encode == null) return handleError();

      postOrderCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }

  private final class CancelAllAgent extends MPSCBufferPollAgent<CancelAllRequestScratch> {
    CancelAllAgent() {
      super(
          cancelAllRequests,
          new CancelAllRequestScratch(),
          CancelAllRequestScratch::copy,
          errorHandler::onCorruptPublication);
    }

    @Override
    protected boolean processRequest(CancelAllRequestScratch scratch) {
      CancelAllRequestEncode encode = singleThreadedClient.claimCancelAll(claimHandle);

      if (encode == null) return handleError();

      cancelAllCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }
}
