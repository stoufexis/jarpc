package stoufexis.sample.generated.client;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.concurrent.*;
import stoufexis.jarpc.lib.client.ringbuffer.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.sample.generated.common.*;

import static stoufexis.jarpc.lib.common.Util.createClientSubscription;
import static stoufexis.jarpc.lib.common.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancellations

public final class LeaseConcurrentJarpcClient
    implements LeaseConcurrentClient, Agent, AutoCloseable, IsConnected {

  private final LeaseSingleThreadedJarpcClient singleThreadedClient;

  private final Long2ObjectHashMap<AcquireResponseHandler> acquireCallbacks;
  private final AcquireRingBuffer acquireRingBuffer;
  private final Long2ObjectHashMap<RefreshResponseHandler> refreshCallbacks;
  private final RefreshRingBuffer refreshRingBuffer;
  private final Long2ObjectHashMap<QueryResponseHandler> queryCallbacks;
  private final QueryRingBuffer queryRingBuffer;

  private final ClientErrorHandler errorHandler;
  private final ConnectivityProbe connectivityProbe;
  private final int queueCapacity;

  LeaseConcurrentJarpcClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {
    this.queueCapacity = queueCapacity;

    this.acquireCallbacks = new Long2ObjectHashMap<>();
    this.acquireRingBuffer = new AcquireRingBuffer();
    this.refreshCallbacks = new Long2ObjectHashMap<>();
    this.refreshRingBuffer = new RefreshRingBuffer();
    this.queryCallbacks = new Long2ObjectHashMap<>();
    this.queryRingBuffer = new QueryRingBuffer();

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
      Aeron aeron, ConnectionConfig cfg, ClientErrorHandler handler, int queueCapacity) {
    Subscription sub = createClientSubscription(aeron, cfg);
    Publication pub = createExclusiveClientPublication(aeron, sub, cfg);
    return new LeaseConcurrentJarpcClient(pub, sub, handler, queueCapacity);
  }

  @Override
  public boolean acquire(AcquireRequestDecode request, AcquireResponseHandler response) {
    return acquireRingBuffer.offer(AcquireRequestScratch::setter, request, response);
  }
  @Override
  public boolean refresh(RefreshRequestDecode request, RefreshResponseHandler response) {
    return refreshRingBuffer.offer(RefreshRequestScratch::setter, request, response);
  }
  @Override
  public boolean query(QueryRequestDecode request, QueryResponseHandler response) {
    return queryRingBuffer.offer(QueryRequestScratch::setter, request, response);
  }

  @Override
  public int doWork() {
    int work = 0;

    connectivityProbe.probeConnected();

    work += acquireRingBuffer.doWork();
    work += refreshRingBuffer.doWork();
    work += queryRingBuffer.doWork();

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

  @Override
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

  private final class AcquireRingBuffer extends MPSCBufferPollAgent<AcquireRequestScratch> {
    AcquireRingBuffer() {
      super(AcquireRequestScratch::new, AcquireRequestScratch::copy, errorHandler, queueCapacity);
    }

    @Override
    protected boolean process(AcquireRequestScratch scratch, OnError onError) {
      AcquireRequestEncode encode = singleThreadedClient.claimAcquire(claimHandle);
      if (encode == null) return onError.run();

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

  private final class RefreshRingBuffer extends MPSCBufferPollAgent<RefreshRequestScratch> {
    RefreshRingBuffer() {
      super(RefreshRequestScratch::new, RefreshRequestScratch::copy, errorHandler, queueCapacity);
    }

    @Override
    protected boolean process(RefreshRequestScratch scratch, OnError onError) {
      RefreshRequestEncode encode = singleThreadedClient.claimRefresh(claimHandle);
      if (encode == null) return onError.run();

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

  private final class QueryRingBuffer extends MPSCBufferPollAgent<QueryRequestScratch> {
    QueryRingBuffer() {
      super(QueryRequestScratch::new, QueryRequestScratch::copy, errorHandler, queueCapacity);
    }

    @Override
    protected boolean process(QueryRequestScratch scratch, OnError onError) {
      QueryRequestEncode encode = singleThreadedClient.claimQuery(claimHandle);
      if (encode == null) return onError.run();

      queryCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }

}

