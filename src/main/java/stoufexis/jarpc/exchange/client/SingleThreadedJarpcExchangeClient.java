package stoufexis.jarpc.exchange.client;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import stoufexis.jarpc.client.SingleThreadedJarpcClient;
import stoufexis.jarpc.exchange.model.CancelAllRequestEncode;
import stoufexis.jarpc.exchange.model.CancelAllResponseDecode;
import stoufexis.jarpc.exchange.model.PostOrderRequestEncode;
import stoufexis.jarpc.exchange.model.PostOrderResponseDecode;
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

  private final PostOrderRequestEncodeSuccess postOrderRequestEncodeSuccess =
      new PostOrderRequestEncodeSuccess();

  private final PostOrderRequestEncodeFail postOrderRequestEncodeFail =
      new PostOrderRequestEncodeFail();

  private final CancelAllRequestEncodeSuccess cancelAllRequestEncodeSuccess =
      new CancelAllRequestEncodeSuccess();

  private final CancelAllRequestEncodeFail cancelAllRequestEncodeFail =
      new CancelAllRequestEncodeFail();

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
      Aeron aeron,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId,
      PostOrderResponseHandler postOrderHandler,
      CancelAllResponseHandler cancelAllHandler,
      ErrorHandler handler) {
    Subscription sub = createClientSubscription(aeron, responseControl, responseStreamId);
    Publication pub =
        createExclusiveClientPublication(aeron, requestEndpoint, requestStreamId, sub);
    return new SingleThreadedJarpcExchangeClient(
        pub, sub, postOrderHandler, cancelAllHandler, handler);
  }

  @Override
  public PostOrderRequestEncode claimPostOrder() {
    ErrorCode result = publisher.tryClaim(POST_ORDER_REQUEST_SIZE);
    if (result != null) {
      postOrderRequestEncodeFail.setErrorCode(result);
      return postOrderRequestEncodeFail;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = publisher.encodeHeader(id, POST_ORDER_MESSAGE_TYPE);
    postOrderRequestEncodeSuccess.setSuccess(id, newOffset, publisher.getClaim());

    return postOrderRequestEncodeSuccess;
  }

  @Override
  public CancelAllRequestEncode claimCancelAll() {
    ErrorCode result = publisher.tryClaim(CANCEL_ALL_REQUEST_SIZE);
    if (result != null) {
      cancelAllRequestEncodeFail.setErrorCode(result);
      return cancelAllRequestEncodeFail;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = publisher.encodeHeader(id, CANCEL_ALL_MESSAGE_TYPE);
    cancelAllRequestEncodeSuccess.setSuccess(id, newOffset, publisher.getClaim());

    return cancelAllRequestEncodeSuccess;
  }

  @Override
  protected boolean onProcessingFailure(int baseMessageType, long correlationId) {
    return switch (baseMessageType) {
      case POST_ORDER_MESSAGE_TYPE -> postOrderResponseHandler.onServerDecodeError(correlationId);
      case CANCEL_ALL_MESSAGE_TYPE -> cancelAllResponseHandler.onServerDecodeError(correlationId);
      default -> throw illegal("Unknown message type " + baseMessageType);
    };
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

    if (length != POST_ORDER_RESPONSE_SIZE) {
      postOrderResponseDecode.set(buffer, offset);
      return postOrderResponseHandler.onResponse(correlationId, postOrderResponseDecode);

    } else {
      return postOrderResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private boolean onCancelAllResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != CANCEL_ALL_RESPONSE_SIZE) {
      cancelAllResponseDecode.set(buffer, offset);
      return cancelAllResponseHandler.onResponse(correlationId, cancelAllResponseDecode);

    } else {
      return cancelAllResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class PostOrderRequestEncodeSuccess extends EncodeSuccessUtil
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

  private static final class PostOrderRequestEncodeFail extends EncodeFailUtil
      implements PostOrderRequestEncode {
    @Override
    public void setBaseAssetId(int baseAssetId) {
      fail();
    }

    @Override
    public void setQuoteAssetId(int quoteAssetId) {
      fail();
    }

    @Override
    public void setQuantityUnscaled(long quantityUnscaled) {
      fail();
    }

    @Override
    public void setQuantityScale(int quantityScale) {
      fail();
    }

    @Override
    public void setRateUnscaled(long rateUnscaled) {
      fail();
    }

    @Override
    public void setRateScale(int rateScale) {
      fail();
    }
  }

  private static final class CancelAllRequestEncodeSuccess extends EncodeSuccessUtil
      implements CancelAllRequestEncode {}

  private static final class CancelAllRequestEncodeFail extends EncodeFailUtil
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
