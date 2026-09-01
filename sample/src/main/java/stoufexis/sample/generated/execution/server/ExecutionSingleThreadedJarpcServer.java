package stoufexis.sample.generated.execution.server;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;
import static stoufexis.jarpc.lib.util.Util.illegal;

import stoufexis.sample.generated.execution.common.*;
import static stoufexis.sample.generated.execution.common.ExecutionMetadata.*;

public class ExecutionSingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements ExecutionSingleThreadedServer, AutoCloseable {


  private final PostLimitOrderRequestHandler postLimitOrderRequestHandler;

  private final PostLimitOrderResponseEncodeImpl postLimitOrderResponseEncode = new PostLimitOrderResponseEncodeImpl();

  private final PostLimitOrderRequestDecodeImpl postLimitOrderRequestDecode = new PostLimitOrderRequestDecodeImpl();

  private final CancelOrderRequestHandler cancelOrderRequestHandler;

  private final CancelOrderResponseEncodeImpl cancelOrderResponseEncode = new CancelOrderResponseEncodeImpl();

  private final CancelOrderRequestDecodeImpl cancelOrderRequestDecode = new CancelOrderRequestDecodeImpl();



  ExecutionSingleThreadedJarpcServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,

      PostLimitOrderRequestHandler postLimitOrderRequestHandler,
      CancelOrderRequestHandler cancelOrderRequestHandler,

      ClientHook clientHook,
      ServerErrorHandler errorHandler) {
    super(subscription, publications, images, clientHook, errorHandler);

    this.postLimitOrderRequestHandler = postLimitOrderRequestHandler;
    this.cancelOrderRequestHandler = cancelOrderRequestHandler;


  }

  public static ExecutionSingleThreadedJarpcServer create(
      ConnectivityConfig cfg,

      PostLimitOrderRequestHandler postLimitOrderRequestHandler,
      CancelOrderRequestHandler cancelOrderRequestHandler,

      ClientHook clientHook,
      ServerErrorHandler serverErrorHandler) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new ExecutionSingleThreadedJarpcServer(
        serverSubscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        images,

        postLimitOrderRequestHandler,
        cancelOrderRequestHandler,

        clientHook,
        serverErrorHandler);
  }

  protected boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length) {
    return switch (messageType) {

      case POST_LIMIT_ORDER_MESSAGE_TYPE -> onPostLimitOrderRequest(clientId, correlationId, buffer, offset, length);
      case CANCEL_ORDER_MESSAGE_TYPE -> onCancelOrderRequest(clientId, correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public PostLimitOrderResponseEncode claimPostLimitOrder(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(POST_LIMIT_ORDER_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, POST_LIMIT_ORDER_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    postLimitOrderResponseEncode.set(claim.buffer(), newOffset);
    return postLimitOrderResponseEncode;
  }

  private boolean onPostLimitOrderRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != POST_LIMIT_ORDER_REQUEST_SIZE) {
      throw illegal("Unable to process PostLimitOrderRequest");
    }

    postLimitOrderRequestDecode.set(buffer, offset);
    return postLimitOrderRequestHandler.onRequest(clientId, correlationId, postLimitOrderRequestDecode);
  }

  @Override
  public CancelOrderResponseEncode claimCancelOrder(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(CANCEL_ORDER_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, CANCEL_ORDER_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    cancelOrderResponseEncode.set(claim.buffer(), newOffset);
    return cancelOrderResponseEncode;
  }

  private boolean onCancelOrderRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != CANCEL_ORDER_REQUEST_SIZE) {
      throw illegal("Unable to process CancelOrderRequest");
    }

    cancelOrderRequestDecode.set(buffer, offset);
    return cancelOrderRequestHandler.onRequest(clientId, correlationId, cancelOrderRequestDecode);
  }




  private static final class PostLimitOrderRequestDecodeImpl extends DecodeUtil
      implements PostLimitOrderRequestDecode {
    @Override
    public int getAssetId() {
      return buffer.getInt(offset + 0);
    }

    @Override
    public long getQuantityUnscaled() {
      return buffer.getLong(offset + 4);
    }

    @Override
    public long getRateUnscaled() {
      return buffer.getLong(offset + 12);
    }

    @Override
    public int getQuantityScale() {
      return buffer.getInt(offset + 20);
    }

    @Override
    public int getRateScale() {
      return buffer.getInt(offset + 24);
    }

  }

  private static final class PostLimitOrderResponseEncodeImpl extends EncodeUtil
      implements PostLimitOrderResponseEncode {
    @Override
    public void setGeneratedId(long generatedId) {
      buffer.putLong(offset + 0, generatedId);
    }

    @Override
    public void setStatusCode(int statusCode) {
      buffer.putInt(offset + 8, statusCode);
    }

  }
  private static final class CancelOrderRequestDecodeImpl extends DecodeUtil
      implements CancelOrderRequestDecode {
    @Override
    public long getGeneratedId() {
      return buffer.getLong(offset + 0);
    }

  }

  private static final class CancelOrderResponseEncodeImpl extends EncodeUtil
      implements CancelOrderResponseEncode {
    @Override
    public void setStatusCode(int statusCode) {
      buffer.putInt(offset + 0, statusCode);
    }

  }

}

