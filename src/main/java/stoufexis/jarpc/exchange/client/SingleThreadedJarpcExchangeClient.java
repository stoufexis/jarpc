package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import stoufexis.jarpc.client.ClientConfig;
import stoufexis.jarpc.util.Publisher;
import stoufexis.jarpc.client.SingleThreadedJarpcClient;
import stoufexis.jarpc.exchange.common.CancelAllRequestEncode;
import stoufexis.jarpc.exchange.common.CancelAllResponseDecode;
import stoufexis.jarpc.exchange.common.PostOrderRequestEncode;
import stoufexis.jarpc.exchange.common.PostOrderResponseDecode;
import stoufexis.jarpc.model.ClaimHandle;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.*;

import static stoufexis.jarpc.util.Util.*;

public final class SingleThreadedJarpcExchangeClient extends SingleThreadedJarpcClient
    implements SingleThreadedExchangeClient {

  private static final int POST_ORDER_REQUEST_SIZE = 32;
  private static final int CANCEL_ALL_REQUEST_SIZE = 0;
  private static final int POST_ORDER_RESPONSE_SIZE = 4;
  private static final int CANCEL_ALL_RESPONSE_SIZE = 4;
  private static final int POST_ORDER_MESSAGE_TYPE = 1;
  private static final int CANCEL_ALL_MESSAGE_TYPE = 2;

  private final PostOrderRequestEncodeImpl postOrderRequestEncode =
      new PostOrderRequestEncodeImpl();

  private final CancelAllRequestEncodeImpl cancelAllRequestEncode =
      new CancelAllRequestEncodeImpl();

  private final PostOrderResponseDecodeImpl postOrderResponseDecode =
      new PostOrderResponseDecodeImpl();

  private final CancelAllResponseDecodeImpl cancelAllResponseDecode =
      new CancelAllResponseDecodeImpl();

  private final PostOrderResponseHandler postOrderResponseHandler;
  private final CancelAllResponseHandler cancelAllResponseHandler;

  SingleThreadedJarpcExchangeClient(
      Publication publication,
      Subscription subscription,
      PostOrderResponseHandler postOrderHandler,
      CancelAllResponseHandler cancelAllHandler,
      ErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);
    this.postOrderResponseHandler = postOrderHandler;
    this.cancelAllResponseHandler = cancelAllHandler;
  }

  public static SingleThreadedJarpcExchangeClient create(
      ClientConfig cfg,
      PostOrderResponseHandler postOrderHandler,
      CancelAllResponseHandler cancelAllHandler,
      ErrorHandler errorHandler) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new SingleThreadedJarpcExchangeClient(
        pub, sub, postOrderHandler, cancelAllHandler, errorHandler);
  }

  @Override
  public PostOrderRequestEncode claimPostOrder(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(POST_ORDER_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, POST_ORDER_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    postOrderRequestEncode.set(claim.buffer(), newOffset);
    return postOrderRequestEncode;
  }

  @Override
  public CancelAllRequestEncode claimCancelAll(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(CANCEL_ALL_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, CANCEL_ALL_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    cancelAllRequestEncode.set(claim.buffer(), newOffset);
    return cancelAllRequestEncode;
  }

  @Override
  protected boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {
    return switch (messageType) {
      case POST_ORDER_MESSAGE_TYPE -> onPostOrderResponse(correlationId, buffer, offset, length);
      case CANCEL_ALL_MESSAGE_TYPE -> onCancelAllResponse(correlationId, buffer, offset, length);
      default -> throw illegal("Unknown message type " + messageType);
    };
  }

  private boolean onPostOrderResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == POST_ORDER_RESPONSE_SIZE) {
      postOrderResponseDecode.set(buffer, offset);
      return postOrderResponseHandler.onResponse(correlationId, postOrderResponseDecode);

    } else {
      return postOrderResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private boolean onCancelAllResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == CANCEL_ALL_RESPONSE_SIZE) {
      cancelAllResponseDecode.set(buffer, offset);
      return cancelAllResponseHandler.onResponse(correlationId, cancelAllResponseDecode);

    } else {
      return cancelAllResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class PostOrderRequestEncodeImpl extends EncodeUtil
      implements PostOrderRequestEncode {
    @Override
    public void setBaseAssetId(int baseAssetId) {
      buffer.putInt(offset, baseAssetId);
    }

    @Override
    public void setQuoteAssetId(int quoteAssetId) {
      buffer.putInt(offset + 4, quoteAssetId);
    }

    @Override
    public void setQuantityUnscaled(long quantityUnscaled) {
      buffer.putLong(offset + 8, quantityUnscaled);
    }

    @Override
    public void setQuantityScale(int quantityScale) {
      buffer.putInt(offset + 16, quantityScale);
    }

    @Override
    public void setRateUnscaled(long rateUnscaled) {
      buffer.putLong(offset + 20, rateUnscaled);
    }

    @Override
    public void setRateScale(int rateScale) {
      buffer.putInt(offset + 28, rateScale);
    }
  }

  private static final class CancelAllRequestEncodeImpl extends EncodeUtil
      implements CancelAllRequestEncode {}

  private static final class PostOrderResponseDecodeImpl extends DecodeUtil
      implements PostOrderResponseDecode {
    @Override
    public int getStatusCode() {
      return buffer.getInt(offset);
    }
  }

  private static final class CancelAllResponseDecodeImpl extends DecodeUtil
      implements CancelAllResponseDecode {
    @Override
    public int getStatusCode() {
      return buffer.getInt(offset);
    }
  }
}
