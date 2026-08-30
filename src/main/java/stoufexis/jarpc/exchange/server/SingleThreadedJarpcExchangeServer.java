package stoufexis.jarpc.exchange.server;

import io.aeron.*;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.util.Publisher;
import stoufexis.jarpc.exchange.common.*;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.server.*;
import stoufexis.jarpc.util.DecodeUtil;
import stoufexis.jarpc.util.EncodeUtil;

import static stoufexis.jarpc.util.Util.createServerSubscription;
import static stoufexis.jarpc.util.Util.illegal;

public class SingleThreadedJarpcExchangeServer extends SingleThreadedJarpcServer
    implements SingleThreadedExchangeServer, AutoCloseable {
  private static final int POST_ORDER_REQUEST_SIZE = 32;
  private static final int CANCEL_ALL_REQUEST_SIZE = 0;
  private static final int POST_ORDER_RESPONSE_SIZE = 4;
  private static final int CANCEL_ALL_RESPONSE_SIZE = 4;
  private static final int POST_ORDER_MESSAGE_TYPE = 1;
  private static final int CANCEL_ALL_MESSAGE_TYPE = 2;

  private final PostOrderRequestHandler postOrderRequestHandler;
  private final CancelAllRequestHandler cancelAllRequestHandler;

  private final PostOrderResponseEncodeImpl postOrderResponseEncode =
      new PostOrderResponseEncodeImpl();

  private final CancelAllResponseEncodeImpl cancelAllResponseEncode =
      new CancelAllResponseEncodeImpl();

  private final PostOrderRequestDecodeImpl postOrderRequestDecode =
      new PostOrderRequestDecodeImpl();

  private final CancelAllRequestDecodeImpl cancelAllRequestDecode =
      new CancelAllRequestDecodeImpl();

  SingleThreadedJarpcExchangeServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,
      ServerErrorHandler errorHandler,
      PostOrderRequestHandler postOrderRequestHandler,
      CancelAllRequestHandler cancelAllRequestHandler) {
    super(subscription, publications, images, errorHandler);
    this.postOrderRequestHandler = postOrderRequestHandler;
    this.cancelAllRequestHandler = cancelAllRequestHandler;
  }

  public static SingleThreadedJarpcExchangeServer create(
      ServerConfig cfg,
      PostOrderRequestHandler postOrderRequestHandler,
      CancelAllRequestHandler cancelAllRequestHandler,
      ServerErrorHandler serverErrorHandler) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new SingleThreadedJarpcExchangeServer(
        serverSubscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        images,
        serverErrorHandler,
        postOrderRequestHandler,
        cancelAllRequestHandler);
  }

  @Override
  public PostOrderResponseEncode claimPostOrder(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(POST_ORDER_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, POST_ORDER_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    postOrderResponseEncode.set(claim.buffer(), newOffset);
    return postOrderResponseEncode;
  }

  @Override
  public CancelAllResponseEncode claimCancelAll(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(CANCEL_ALL_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, CANCEL_ALL_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    cancelAllResponseEncode.set(claim.buffer(), newOffset);
    return cancelAllResponseEncode;
  }

  protected boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length) {
    return switch (messageType) {
      case POST_ORDER_MESSAGE_TYPE ->
          onPostOrderRequest(clientId, correlationId, buffer, offset, length);
      case CANCEL_ALL_MESSAGE_TYPE ->
          onCancelAllRequest(clientId, correlationId, buffer, offset, length);
      default -> throw illegal("Unknown message type " + messageType);
    };
  }

  private boolean onPostOrderRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != POST_ORDER_REQUEST_SIZE) {
      throw illegal("Unable to process PostOrderRequest");
    }

    postOrderRequestDecode.set(buffer, offset);
    return postOrderRequestHandler.onRequest(clientId, correlationId, postOrderRequestDecode);
  }

  private boolean onCancelAllRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != CANCEL_ALL_REQUEST_SIZE) {
      throw illegal("Unable to process CancelAllRequest");
    }

    cancelAllRequestDecode.set(buffer, offset);
    return cancelAllRequestHandler.onRequest(clientId, correlationId, cancelAllRequestDecode);
  }

  private static final class PostOrderResponseEncodeImpl extends EncodeUtil
      implements PostOrderResponseEncode {
    @Override
    public void setStatusCode(int statusCode) {
      buffer.putInt(offset, statusCode);
    }
  }

  private static final class CancelAllResponseEncodeImpl extends EncodeUtil
      implements CancelAllResponseEncode {
    @Override
    public void setStatusCode(int statusCode) {
      buffer.putInt(offset, statusCode);
    }
  }

  private static final class PostOrderRequestDecodeImpl extends DecodeUtil
      implements PostOrderRequestDecode {
    @Override
    public int getBaseAssetId() {
      return buffer.getInt(offset);
    }

    @Override
    public int getQuoteAssetId() {
      return buffer.getInt(offset + 4);
    }

    @Override
    public long getQuantityUnscaled() {
      return buffer.getLong(offset + 8);
    }

    @Override
    public int getQuantityScale() {
      return buffer.getInt(offset + 16);
    }

    @Override
    public long getRateUnscaled() {
      return buffer.getLong(offset + 20);
    }

    @Override
    public int getRateScale() {
      return buffer.getInt(offset + 28);
    }
  }

  private static final class CancelAllRequestDecodeImpl extends DecodeUtil
      implements CancelAllRequestDecode {}
}
