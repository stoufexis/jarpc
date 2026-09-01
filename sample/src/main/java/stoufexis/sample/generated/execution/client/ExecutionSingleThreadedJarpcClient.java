package stoufexis.sample.generated.execution.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;

import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;

import stoufexis.jarpc.lib.util.*;
import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import static stoufexis.jarpc.lib.util.Util.*;

import static stoufexis.sample.generated.execution.common.ExecutionMetadata.*;
import stoufexis.sample.generated.execution.common.*;

public final class ExecutionSingleThreadedJarpcClient extends SingleThreadedJarpcClient
    implements ExecutionSingleThreadedClient {


  private final PostLimitOrderRequestEncodeImpl postLimitOrderRequestEncode = new PostLimitOrderRequestEncodeImpl();

  private final PostLimitOrderResponseDecodeImpl postLimitOrderResponseDecode = new PostLimitOrderResponseDecodeImpl();

  private final PostLimitOrderResponseHandler postLimitOrderResponseHandler;

  private final CancelOrderRequestEncodeImpl cancelOrderRequestEncode = new CancelOrderRequestEncodeImpl();

  private final CancelOrderResponseDecodeImpl cancelOrderResponseDecode = new CancelOrderResponseDecodeImpl();

  private final CancelOrderResponseHandler cancelOrderResponseHandler;



  ExecutionSingleThreadedJarpcClient(
      Publication publication,
      Subscription subscription,

      PostLimitOrderResponseHandler postLimitOrderHandler,
      CancelOrderResponseHandler cancelOrderHandler,

      ErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);

    this.postLimitOrderResponseHandler = postLimitOrderHandler;
    this.cancelOrderResponseHandler = cancelOrderHandler;

  }

  public static ExecutionSingleThreadedJarpcClient create(
      ConnectivityConfig cfg,

      PostLimitOrderResponseHandler postLimitOrderHandler,
      CancelOrderResponseHandler cancelOrderHandler,

      ErrorHandler errorHandler) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new ExecutionSingleThreadedJarpcClient(
        pub,
        sub,

        postLimitOrderHandler,
        cancelOrderHandler,

        errorHandler);
  }

  @Override
  protected boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {
    return switch (messageType) {

      case POST_LIMIT_ORDER_MESSAGE_TYPE -> onPostLimitOrderResponse(correlationId, buffer, offset, length);
      case CANCEL_ORDER_MESSAGE_TYPE -> onCancelOrderResponse(correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public PostLimitOrderRequestEncode claimPostLimitOrder(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(POST_LIMIT_ORDER_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, POST_LIMIT_ORDER_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    postLimitOrderRequestEncode.set(claim.buffer(), newOffset);
    return postLimitOrderRequestEncode;
  }

  private boolean onPostLimitOrderResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == POST_LIMIT_ORDER_RESPONSE_SIZE) {
      postLimitOrderResponseDecode.set(buffer, offset);
      return postLimitOrderResponseHandler.onResponse(correlationId, postLimitOrderResponseDecode);

    } else {
      return postLimitOrderResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class PostLimitOrderRequestEncodeImpl extends EncodeUtil
      implements PostLimitOrderRequestEncode {
    @Override
    public void setAssetId(int assetId) {
      buffer.putInt(offset + 0, assetId);
    }

    @Override
    public void setQuantityUnscaled(long quantityUnscaled) {
      buffer.putLong(offset + 4, quantityUnscaled);
    }

    @Override
    public void setRateUnscaled(long rateUnscaled) {
      buffer.putLong(offset + 12, rateUnscaled);
    }

    @Override
    public void setQuantityScale(int quantityScale) {
      buffer.putInt(offset + 20, quantityScale);
    }

    @Override
    public void setRateScale(int rateScale) {
      buffer.putInt(offset + 24, rateScale);
    }

  }

  private static final class PostLimitOrderResponseDecodeImpl extends DecodeUtil
      implements PostLimitOrderResponseDecode {
    @Override
    public long getGeneratedId() {
      return buffer.getLong(offset + 0);
    }

    @Override
    public int getStatusCode() {
      return buffer.getInt(offset + 8);
    }

  }
  @Override
  public CancelOrderRequestEncode claimCancelOrder(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(CANCEL_ORDER_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, CANCEL_ORDER_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    cancelOrderRequestEncode.set(claim.buffer(), newOffset);
    return cancelOrderRequestEncode;
  }

  private boolean onCancelOrderResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == CANCEL_ORDER_RESPONSE_SIZE) {
      cancelOrderResponseDecode.set(buffer, offset);
      return cancelOrderResponseHandler.onResponse(correlationId, cancelOrderResponseDecode);

    } else {
      return cancelOrderResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class CancelOrderRequestEncodeImpl extends EncodeUtil
      implements CancelOrderRequestEncode {
    @Override
    public void setGeneratedId(long generatedId) {
      buffer.putLong(offset + 0, generatedId);
    }

  }

  private static final class CancelOrderResponseDecodeImpl extends DecodeUtil
      implements CancelOrderResponseDecode {
    @Override
    public int getStatusCode() {
      return buffer.getInt(offset + 0);
    }

  }

}

