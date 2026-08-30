package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.client.ClientAgent;
import stoufexis.jarpc.client.ClientConfig;
import stoufexis.jarpc.client.ClientErrorHandler;
import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.util.MPSCRingBuffer;
import stoufexis.jarpc.util.ResponseHandlerUtil;

import static stoufexis.jarpc.util.Util.createClientSubscription;
import static stoufexis.jarpc.util.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancel

public final class ConcurrentJarpcExchangeClient implements ConcurrentExchangeClient, Agent {

  private final SingleThreadedJarpcExchangeClient singleThreadedClient;

  private final MPSCRingBuffer<PostOrderRequestScratch> postOrderRequests;
  private final MPSCRingBuffer<CancelAllRequestScratch> cancelAllRequests;

  private final ClientErrorHandler errorHandler;

  private final Long2ObjectHashMap<PostOrderResponseHandler> postOrderCallbacks;
  private final Long2ObjectHashMap<CancelAllResponseHandler> cancelAllCallbacks;

  private final PostOrderClientAgent postOrderClientAgent;
  private final CancelAllClientAgent cancelAllClientAgent;

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
    this.postOrderClientAgent = new PostOrderClientAgent();
    this.cancelAllClientAgent = new CancelAllClientAgent();
  }

  public static ConcurrentJarpcExchangeClient create(
      ClientConfig cfg, ClientErrorHandler handler, int queueCapacity) {
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

  private final class PostOrderHandler extends ResponseHandlerUtil<PostOrderResponseHandler>
      implements PostOrderResponseHandler {
    PostOrderHandler() {
      super(postOrderCallbacks, errorHandler, "PostOrder");
    }

    @Override
    public boolean onResponse(long correlationId, PostOrderResponseDecode t) {
      PostOrderResponseHandler callback = getCallback(correlationId);

      if (callback == null) return true;

      boolean dispatched = callback.onResponse(correlationId, t);

      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class CancelAllHandler extends ResponseHandlerUtil<CancelAllResponseHandler>
      implements CancelAllResponseHandler {
    CancelAllHandler() {
      super(cancelAllCallbacks, errorHandler, "CancelAll");
    }

    @Override
    public boolean onResponse(long correlationId, CancelAllResponseDecode t) {
      CancelAllResponseHandler callback = getCallback(correlationId);

      if (callback == null) return true;

      boolean dispatched = callback.onResponse(correlationId, t);

      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class PostOrderClientAgent extends ClientAgent<PostOrderRequestScratch> {
    PostOrderClientAgent() {
      super(
          postOrderRequests,
          new PostOrderRequestScratch(),
          PostOrderRequestScratch::copy,
          errorHandler);
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

  private final class CancelAllClientAgent extends ClientAgent<CancelAllRequestScratch> {
    CancelAllClientAgent() {
      super(
          cancelAllRequests,
          new CancelAllRequestScratch(),
          CancelAllRequestScratch::copy,
          errorHandler);
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

  @Override
  public int doWork() {
    int work = 0;
    // Use this instead of CompositeAgent to monomorphize all calls to doWork
    // FIXME verify rationale
    work += postOrderClientAgent.doWork();
    work += cancelAllClientAgent.doWork();
    work += singleThreadedClient.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "ConcurrentJarpcExchangeClient";
  }
}
