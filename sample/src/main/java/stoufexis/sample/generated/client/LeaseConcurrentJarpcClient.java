package stoufexis.sample.generated.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.sample.generated.common.*;

import static stoufexis.jarpc.lib.common.Util.createClientSubscription;
import static stoufexis.jarpc.lib.common.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancel

public final class LeaseConcurrentJarpcClient
    implements LeaseConcurrentClient, Agent, AutoCloseable {

  private final LeaseSingleThreadedJarpcClient singleThreadedClient;

  private final MPSCRingBuffer<AcquireRequestScratch> acquireRequests;
  private final Long2ObjectHashMap<AcquireResponseHandler> acquireCallbacks;
  private final AcquireAgent acquireAgent;
  private final MPSCRingBuffer<RefreshRequestScratch> refreshRequests;
  private final Long2ObjectHashMap<RefreshResponseHandler> refreshCallbacks;
  private final RefreshAgent refreshAgent;
  private final MPSCRingBuffer<QueryRequestScratch> queryRequests;
  private final Long2ObjectHashMap<QueryResponseHandler> queryCallbacks;
  private final QueryAgent queryAgent;

  private final ClientErrorHandler errorHandler;
  private final ConnectivityProbe connectivityProbe;

  LeaseConcurrentJarpcClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {

    this.acquireCallbacks = new Long2ObjectHashMap<>();
    this.acquireRequests = new MPSCRingBuffer<>(queueCapacity, AcquireRequestScratch::new);
    this.acquireAgent = new AcquireAgent();
    this.refreshCallbacks = new Long2ObjectHashMap<>();
    this.refreshRequests = new MPSCRingBuffer<>(queueCapacity, RefreshRequestScratch::new);
    this.refreshAgent = new RefreshAgent();
    this.queryCallbacks = new Long2ObjectHashMap<>();
    this.queryRequests = new MPSCRingBuffer<>(queueCapacity, QueryRequestScratch::new);
    this.queryAgent = new QueryAgent();

    this.singleThreadedClient =
        new LeaseSingleThreadedJarpcClient(
            publication,
            subscription,

            new AcquireHandler(),
            new RefreshHandler(),
            new QueryHandler(),

            errorHandler);
    this.errorHandler = errorHandler;
    this.connectivityProbe = new ConnectivityProbe(singleThreadedClient);
  }

  public static LeaseConcurrentJarpcClient create(
      ConnectionConfig cfg, ClientErrorHandler handler, int queueCapacity) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new LeaseConcurrentJarpcClient(pub, sub, handler, queueCapacity);
  }

  @Override
  public boolean acquire(AcquireRequestDecode request, AcquireResponseHandler response) {
    return acquireRequests.offer(AcquireRequestScratch::setter, request, response);
  }
  @Override
  public boolean refresh(RefreshRequestDecode request, RefreshResponseHandler response) {
    return refreshRequests.offer(RefreshRequestScratch::setter, request, response);
  }
  @Override
  public boolean query(QueryRequestDecode request, QueryResponseHandler response) {
    return queryRequests.offer(QueryRequestScratch::setter, request, response);
  }

  @Override
  public int doWork() {
    int work = 0;

    connectivityProbe.probeConnected();

    // Use this instead of CompositeAgent to monomorphize all calls to doWork

    work += acquireAgent.doWork();
    work += refreshAgent.doWork();
    work += queryAgent.doWork();

    work += singleThreadedClient.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "LeaseConcurrentJarpcClient";
  }

  @Override
  public void close() {
    singleThreadedClient.close();
  }

  public boolean isConnected() {
    return connectivityProbe.isConnected();
  }


  private final class AcquireHandler extends ResponseHandlerUtil<AcquireResponseHandler>
      implements LeaseSingleThreadedJarpcClient.AcquireResponseHandler {
    AcquireHandler() {
      super(acquireCallbacks, errorHandler, "Acquire");
    }

    @Override
    public boolean onResponse(long correlationId, AcquireResponseDecode t) {
      AcquireResponseHandler callback = getCallback(correlationId);
      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);
      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class AcquireAgent extends MPSCBufferPollAgent<AcquireRequestScratch> {
    AcquireAgent() {
      super(acquireRequests, new AcquireRequestScratch(), AcquireRequestScratch::copy, errorHandler);
    }

    @Override
    protected boolean process(AcquireRequestScratch scratch) {
      AcquireRequestEncode encode = singleThreadedClient.claimAcquire(claimHandle);
      if (encode == null) return handleError();

      acquireCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }
  private final class RefreshHandler extends ResponseHandlerUtil<RefreshResponseHandler>
      implements LeaseSingleThreadedJarpcClient.RefreshResponseHandler {
    RefreshHandler() {
      super(refreshCallbacks, errorHandler, "Refresh");
    }

    @Override
    public boolean onResponse(long correlationId, RefreshResponseDecode t) {
      RefreshResponseHandler callback = getCallback(correlationId);
      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);
      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class RefreshAgent extends MPSCBufferPollAgent<RefreshRequestScratch> {
    RefreshAgent() {
      super(refreshRequests, new RefreshRequestScratch(), RefreshRequestScratch::copy, errorHandler);
    }

    @Override
    protected boolean process(RefreshRequestScratch scratch) {
      RefreshRequestEncode encode = singleThreadedClient.claimRefresh(claimHandle);
      if (encode == null) return handleError();

      refreshCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }
  private final class QueryHandler extends ResponseHandlerUtil<QueryResponseHandler>
      implements LeaseSingleThreadedJarpcClient.QueryResponseHandler {
    QueryHandler() {
      super(queryCallbacks, errorHandler, "Query");
    }

    @Override
    public boolean onResponse(long correlationId, QueryResponseDecode t) {
      QueryResponseHandler callback = getCallback(correlationId);
      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);
      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class QueryAgent extends MPSCBufferPollAgent<QueryRequestScratch> {
    QueryAgent() {
      super(queryRequests, new QueryRequestScratch(), QueryRequestScratch::copy, errorHandler);
    }

    @Override
    protected boolean process(QueryRequestScratch scratch) {
      QueryRequestEncode encode = singleThreadedClient.claimQuery(claimHandle);
      if (encode == null) return handleError();

      queryCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }

}

