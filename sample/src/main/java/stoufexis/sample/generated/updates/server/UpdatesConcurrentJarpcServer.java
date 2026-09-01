package stoufexis.sample.generated.updates.server;

import io.aeron.Subscription;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;

import stoufexis.sample.generated.updates.common.*;

public final class UpdatesConcurrentJarpcServer implements Agent, AutoCloseable {

  private final UpdatesSingleThreadedJarpcServer singleThreadedServer;

  private final UpdatesConcurrentServer concurrentUpdatesServer;

  private final ServerErrorHandler errorHandler;


  private final UpdatesConcurrentServer.SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesResponseHandler;
  private final MPSCRingBuffer<SubscribeOrderUpdatesResponseScratch> subscribeOrderUpdatesResponses;
  private final SubscribeOrderUpdatesAgent subscribeOrderUpdatesAgent;


  UpdatesConcurrentJarpcServer(
      UpdatesConcurrentServer concurrentUpdatesServer,
      Subscription subscription,
      ServerPublications publications,
      ServerErrorHandler errorHandler,
      Images images,
      int queueCapacity) {
    this.errorHandler = errorHandler;
    this.concurrentUpdatesServer = concurrentUpdatesServer;
    this.singleThreadedServer =
        new UpdatesSingleThreadedJarpcServer(
            subscription,
            publications,
            images,

            new SubscribeOrderUpdatesRequestHandler(),

            concurrentUpdatesServer,
            errorHandler);

    this.subscribeOrderUpdatesResponseHandler = new SubscribeOrderUpdatesResponseHandler();
    this.subscribeOrderUpdatesResponses = new MPSCRingBuffer<>(queueCapacity, SubscribeOrderUpdatesResponseScratch::new);
    this.subscribeOrderUpdatesAgent = new SubscribeOrderUpdatesAgent();

  }

  public static UpdatesConcurrentJarpcServer create(
      UpdatesConcurrentServer concurrentUpdatesServer,
      ConnectivityConfig cfg,
      ServerErrorHandler errorHandler,
      int queueCapacity) {
    Images images = new Images();

    Subscription subscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new UpdatesConcurrentJarpcServer(
        concurrentUpdatesServer,
        subscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        errorHandler,
        images,
        queueCapacity);
  }

  @Override
  public void onStart() {

    concurrentUpdatesServer.registerSubscribeOrderUpdates(subscribeOrderUpdatesResponseHandler);

  }

  @Override
  public int doWork() {
    int work = 0;
    work += singleThreadedServer.poll(1);

    work += subscribeOrderUpdatesAgent.doWork();

    return work;
  }

  @Override
  public String roleName() {
    return "ConcurrentJarpcUpdatesServer";
  }

  @Override
  public void close() {
    singleThreadedServer.close();
  }


  private final class SubscribeOrderUpdatesRequestHandler
      implements UpdatesSingleThreadedServer.SubscribeOrderUpdatesRequestHandler {
    @Override
    public boolean onRequest(long clientId, long correlationId, SubscribeOrderUpdatesRequestDecode t) {
      return concurrentUpdatesServer.subscribeOrderUpdates(clientId, correlationId, t);
    }
  }

  private final class SubscribeOrderUpdatesResponseHandler
      implements UpdatesConcurrentServer.SubscribeOrderUpdatesResponseHandler {
    @Override
    public boolean onResponse(long clientId, long correlationId, SubscribeOrderUpdatesResponseDecode t) {
      return subscribeOrderUpdatesResponses.offer(SubscribeOrderUpdatesResponseScratch::setter, t, clientId, correlationId);
    }
  }

  private final class SubscribeOrderUpdatesAgent extends MPSCBufferPollAgent<SubscribeOrderUpdatesResponseScratch> {
    SubscribeOrderUpdatesAgent() {
      super(
          subscribeOrderUpdatesResponses, new SubscribeOrderUpdatesResponseScratch(), SubscribeOrderUpdatesResponseScratch::copy, errorHandler);
    }

    @Override
    protected boolean processRequest(SubscribeOrderUpdatesResponseScratch scratch) {
      SubscribeOrderUpdatesResponseEncode encode =
          singleThreadedServer.claimSubscribeOrderUpdates(
              scratch.getClientId(), scratch.getCorrelationId(), claimHandle);

      if (encode == null) return handleError();

      encode.set(scratch);
      return true;
    }
  }

}

