package stoufexis.jarpc.lib.integration.generated.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.concurrent.*;
import stoufexis.jarpc.lib.client.ringbuffer.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.jarpc.lib.integration.generated.common.*;

import static stoufexis.jarpc.lib.common.Util.createClientSubscription;
import static stoufexis.jarpc.lib.common.Util.createExclusiveClientPublication;

// FIXME add timeouts and ad-hoc cancel

public final class EchoConcurrentJarpcClient
    implements EchoConcurrentClient, Agent, AutoCloseable, IsConnected {

  private final EchoSingleThreadedJarpcClient singleThreadedClient;

  private final Long2ObjectHashMap<EchoResponseHandler> echoCallbacks;
  private final EchoRingBuffer echoRingBuffer;

  private final ClientErrorHandler errorHandler;
  private final ConnectivityProbe connectivityProbe;
  private final int queueCapacity;

  EchoConcurrentJarpcClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {
    this.queueCapacity = queueCapacity;

    this.echoCallbacks = new Long2ObjectHashMap<>();
    this.echoRingBuffer = new EchoRingBuffer();

    this.singleThreadedClient =
        new EchoSingleThreadedJarpcClient(
            publication,
            subscription,

            new EchoHandler(),

            errorHandler);
    this.errorHandler = errorHandler;
    this.connectivityProbe = new ConnectivityProbe(singleThreadedClient);
  }

  public static EchoConcurrentJarpcClient create(
      ConnectionConfig cfg, ClientErrorHandler handler, int queueCapacity) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new EchoConcurrentJarpcClient(pub, sub, handler, queueCapacity);
  }

  @Override
  public boolean echo(EchoRequestDecode request, EchoResponseHandler response) {
    return echoRingBuffer.offer(EchoRequestScratch::setter, request, response);
  }

  @Override
  public int doWork() {
    int work = 0;

    connectivityProbe.probeConnected();

    work += echoRingBuffer.doWork();

    work += singleThreadedClient.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "EchoConcurrentJarpcClient";
  }

  @Override
  public void close() {
    singleThreadedClient.close();
  }

  @Override
  public boolean isConnected() {
    return connectivityProbe.isConnected();
  }

  private final class EchoHandler extends ResponseHandlerUtil<EchoResponseHandler>
      implements EchoSingleThreadedJarpcClient.EchoResponseHandler {
    EchoHandler() {
      super(echoCallbacks, errorHandler, "Echo");
    }

    @Override
    public boolean onResponse(long correlationId, EchoResponseDecode t) {
      EchoResponseHandler callback = getCallback(correlationId);
      if (callback == null) return true;

      boolean dispatched = callback.onResponse(t);
      if (dispatched) removeCallback(correlationId);

      return dispatched;
    }
  }

  private final class EchoRingBuffer extends MPSCBufferPollAgent<EchoRequestScratch> {
    EchoRingBuffer() {
      super(EchoRequestScratch::new, EchoRequestScratch::copy, errorHandler, queueCapacity);
    }

    @Override
    protected boolean process(EchoRequestScratch scratch, OnError onError) {
      EchoRequestEncode encode = singleThreadedClient.claimEcho(claimHandle);
      if (encode == null) return onError.run();

      echoCallbacks.put(claimHandle.getCorrelationId(), scratch.getHandler());
      encode.set(scratch);
      return true;
    }
  }

}

