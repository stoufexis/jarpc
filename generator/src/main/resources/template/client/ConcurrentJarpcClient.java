package _package_.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.ringbuffer.*;
import stoufexis.jarpc.lib.common.*;

import _package_.common.*;

import static stoufexis.jarpc.lib.common.Util.createClientSubscription;
import static stoufexis.jarpc.lib.common.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancel

public final class _Service_ConcurrentJarpcClient
    implements _Service_ConcurrentClient, Agent, AutoCloseable {

  private final _Service_SingleThreadedJarpcClient singleThreadedClient;
  /// foreachType
  private final Long2ObjectHashMap<_Type_ResponseHandler> _type_Callbacks;
  private final _Type_RingBuffer _type_RingBuffer;
  /// foreachType
  private final ClientErrorHandler errorHandler;
  private final ConnectivityProbe connectivityProbe;
  private final int queueCapacity;

  _Service_ConcurrentJarpcClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {
    /// foreachType
    this._type_Callbacks = new Long2ObjectHashMap<>();
    this._type_RingBuffer = new _Type_RingBuffer();
    /// foreachType
    this.singleThreadedClient =
        new _Service_SingleThreadedJarpcClient(
            publication,
            subscription,
            /// foreachType
            new _Type_Handler(),
            /// foreachType
            errorHandler);
    this.errorHandler = errorHandler;
    this.connectivityProbe = new ConnectivityProbe(singleThreadedClient);
    this.queueCapacity = queueCapacity;
  }

  public static _Service_ConcurrentJarpcClient create(
      ConnectionConfig cfg, ClientErrorHandler handler, int queueCapacity) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new _Service_ConcurrentJarpcClient(pub, sub, handler, queueCapacity);
  }
  /// foreachType
  @Override
  public boolean _type_(_Type_RequestDecode request, _Type_ResponseHandler response) {
    return _type_RingBuffer.offer(_Type_RequestScratch::setter, request, response);
  }
  /// foreachType
  @Override
  public int doWork() {
    int work = 0;

    connectivityProbe.probeConnected();
    /// foreachType
    work += _type_RingBuffer.doWork();
    /// foreachType
    work += singleThreadedClient.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "_Service_ConcurrentJarpcClient";
  }

  @Override
  public void close() {
    singleThreadedClient.close();
  }

  public boolean isConnected() {
    return connectivityProbe.isConnected();
  }

  /// foreachType
  private final class _Type_Handler extends ResponseHandlerUtil<_Type_ResponseHandler>
      implements _Service_SingleThreadedJarpcClient._Type_ResponseHandler {
    _Type_Handler() {
      super(_type_Callbacks, errorHandler, "_Type_");
    }

    @Override
    public boolean onResponse(long correlationId, _Type_ResponseDecode t) {
      _Type_ResponseHandler callback = getCallback(correlationId);
      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);
      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class _Type_RingBuffer extends MPSCBufferPollAgent<_Type_RequestScratch> {
    _Type_RingBuffer() {
      super(_Type_RequestScratch::new, _Type_RequestScratch::copy, errorHandler, queueCapacity);
    }

    @Override
    protected boolean process(_Type_RequestScratch scratch, OnError onError) {
      _Type_RequestEncode encode = singleThreadedClient.claim_Type_(claimHandle);
      if (encode == null) return onError.run();

      _type_Callbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }
  /// foreachType
}
