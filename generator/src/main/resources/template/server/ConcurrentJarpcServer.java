package _package_.server;

import io.aeron.Subscription;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;

import _package_.common.*;

public final class _Service_ConcurrentJarpcServer implements Agent, AutoCloseable {

  private final _Service_SingleThreadedJarpcServer singleThreadedServer;

  private final _Service_ConcurrentServer concurrent_Service_Server;

  private final ServerErrorHandler errorHandler;

  private final _Service_ConcurrentServer.PostOrderResponseHandler postOrderResponseHandler;
  private final _Service_ConcurrentServer.CancelAllResponseHandler cancelAllResponseHandler;

  private final MPSCRingBuffer<PostOrderResponseScratch> postOrderResponses;
  private final MPSCRingBuffer<CancelAllResponseScratch> cancelAllResponses;

  private final PostOrderAgent postOrderAgent;
  private final CancelAllAgent cancelAllAgent;

  _Service_ConcurrentJarpcServer(
      _Service_ConcurrentServer concurrent_Service_Server,
      Subscription subscription,
      ServerPublications publications,
      ServerErrorHandler errorHandler,
      Images images,
      int queueCapacity) {
    this.errorHandler = errorHandler;
    this.concurrent_Service_Server = concurrent_Service_Server;
    this.singleThreadedServer =
        new _Service_SingleThreadedJarpcServer(
            subscription,
            publications,
            images,
            /// foreachType
            new _Type_RequestHandler(),
            /// foreachType
            errorHandler);
    /// foreachType
    this._type_ResponseHandler = new _Type_ResponseHandler();
    this._type_Responses = new MPSCRingBuffer<>(queueCapacity, _Type_ResponseScratch::new);
    this._type_Agent = new _Type_Agent();
    /// foreachType
  }

  public static _Service_ConcurrentJarpcServer create(
      _Service_ConcurrentServer concurrent_Service_Server,
      ConnectivityConfig cfg,
      ServerErrorHandler errorHandler,
      int queueCapacity) {
    Images images = new Images();

    Subscription subscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new _Service_ConcurrentJarpcServer(
        concurrent_Service_Server,
        subscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        errorHandler,
        images,
        queueCapacity);
  }

  @Override
  public void onStart() {
    /// foreachType
    concurrent_Service_Server.register_Type_(_type_ResponseHandler);
    /// foreachType
  }

  @Override
  public int doWork() {
    int work = 0;
    work += singleThreadedServer.poll(1);
    /// foreachType
    work += _type_Agent.doWork();
    /// foreachType
    return work;
  }

  @Override
  public String roleName() {
    return "ConcurrentJarpc_Service_Server";
  }

  @Override
  public void close() {
    singleThreadedServer.close();
  }

  /// foreachType
  private final class _Type_RequestHandler
      implements _Service_SingleThreadedServer._Type_RequestHandler {
    @Override
    public boolean onRequest(long clientId, long correlationId, _Type_RequestDecode t) {
      return concurrent_Service_Server._type_(clientId, correlationId, t);
    }
  }

  private final class _Type_ResponseHandler
      implements _Service_ConcurrentServer._Type_ResponseHandler {
    @Override
    public boolean onResponse(long clientId, long correlationId, _Type_ResponseDecode t) {
      return _type_Responses.offer(_Type_ResponseScratch::setter, t, clientId, correlationId);
    }
  }

  private final class _Type_Agent extends MPSCBufferPollAgent<_Type_ResponseScratch> {
    _Type_Agent() {
      super(
          _type_Responses, new _Type_ResponseScratch(), _Type_ResponseScratch::copy, errorHandler);
    }

    @Override
    protected boolean processRequest(_Type_ResponseScratch scratch) {
      _Type_ResponseEncode encode =
          singleThreadedServer.claim_Type_(
              scratch.getClientId(), scratch.getCorrelationId(), claimHandle);

      if (encode == null) return handleError();

      encode.set(scratch);
      return true;
    }
  }
  /// foreachType
}
