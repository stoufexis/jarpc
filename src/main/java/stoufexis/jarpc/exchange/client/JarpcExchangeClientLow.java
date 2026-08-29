package stoufexis.jarpc.exchange.client;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.client.JarpcClient;
import stoufexis.jarpc.client.PollFragmentHandler;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.MessageHeaderCodec;
import stoufexis.jarpc.util.*;

import static stoufexis.jarpc.util.Util.*;
import static stoufexis.jarpc.util.Util.createClientPublication;

/** Meant to be used by a single thread. */
public class JarpcExchangeClientLow extends JarpcClient implements ExchangeClientLow {
  private static final int POST_ORDER_REQUEST_SIZE = 32;
  private static final int CANCEL_ALL_REQUEST_SIZE = 0;
  private static final int POST_ORDER_RESPONSE_SIZE = 4;
  private static final int CANCEL_ALL_RESPONSE_SIZE = 4;
  private static final int POST_ORDER_MESSAGE_TYPE = 1;
  private static final int CANCEL_ALL_MESSAGE_TYPE = 2;

  private long correlationId = 0;

  private final BufferClaim claim = new BufferClaim();

  private final PostOrderRequestEncodeSuccess postOrderRequestEncodeSuccess =
      new PostOrderRequestEncodeSuccess();

  private final PostOrderRequestEncodeFail postOrderRequestEncodeFail =
      new PostOrderRequestEncodeFail();

  private final CancelAllRequestEncodeSuccess cancelAllRequestEncodeSuccess =
      new CancelAllRequestEncodeSuccess();

  private final CancelAllRequestEncodeFail cancelAllRequestEncodeFail =
      new CancelAllRequestEncodeFail();

  JarpcExchangeClientLow(
      Publication publication,
      Subscription subscription,
      PostOrderResponseHandler postOrderHandler,
      CancelAllResponseHandler cancelAllHandler,
      ErrorHandler errorHandler) {
    super(
        publication,
        subscription,
        new ExchangeFragmentHandler(errorHandler, postOrderHandler, cancelAllHandler));
  }

  public static JarpcExchangeClientLow create(
      Aeron aeron,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId,
      PostOrderResponseHandler postOrderHandler,
      CancelAllResponseHandler cancelAllHandler,
      ErrorHandler handler) {
    Subscription sub = createClientSubscription(aeron, responseControl, responseStreamId);
    Publication pub = createClientPublication(aeron, requestEndpoint, requestStreamId, sub);
    return new JarpcExchangeClientLow(pub, sub, postOrderHandler, cancelAllHandler, handler);
  }

  @Override
  public PostOrderRequestEncode claimPostOrder() {
    long result = publication.tryClaim(POST_ORDER_REQUEST_SIZE + MessageHeaderCodec.HEADER_SIZE, claim);

    if (result < 0) {
      postOrderRequestEncodeFail.setErrorCode(interpretErrorCode(result));
      return postOrderRequestEncodeFail;
    }

    long id = correlationId++;
    int offset = claim.offset();
    MutableDirectBuffer buffer = claim.buffer();
    MessageHeaderCodec.encode(buffer, offset, id, POST_ORDER_MESSAGE_TYPE);
    postOrderRequestEncodeSuccess.setSuccess(id, buffer, offset + MessageHeaderCodec.HEADER_SIZE, claim);

    return postOrderRequestEncodeSuccess;
  }

  @Override
  public CancelAllRequestEncode claimCancelAll() {
    long result = publication.tryClaim(CANCEL_ALL_REQUEST_SIZE + MessageHeaderCodec.HEADER_SIZE, claim);

    if (result < 0) {
      cancelAllRequestEncodeFail.setErrorCode(interpretErrorCode(result));
      return cancelAllRequestEncodeFail;
    }

    long id = correlationId++;
    int offset = claim.offset();
    MutableDirectBuffer buffer = claim.buffer();
    MessageHeaderCodec.encode(buffer, offset, id, CANCEL_ALL_MESSAGE_TYPE);
    cancelAllRequestEncodeSuccess.setSuccess(id, buffer, offset + MessageHeaderCodec.HEADER_SIZE, claim);

    return cancelAllRequestEncodeSuccess;
  }

  @Override
  public int poll(int limit) {
    return super.poll(limit);
  }

  private static final class ExchangeFragmentHandler extends PollFragmentHandler {
    private final PostOrderResponseDecodeImpl postOrderResponseDecode =
        new PostOrderResponseDecodeImpl();

    private final CancelAllResponseDecodeImpl cancelAllResponseDecode =
        new CancelAllResponseDecodeImpl();

    private final PostOrderResponseHandler postOrderResponseHandler;
    private final CancelAllResponseHandler cancelAllResponseHandler;

    private ExchangeFragmentHandler(
        ErrorHandler handler,
        PostOrderResponseHandler postOrderResponseHandler,
        CancelAllResponseHandler cancelAllResponseHandler) {
      super(handler);
      this.postOrderResponseHandler = postOrderResponseHandler;
      this.cancelAllResponseHandler = cancelAllResponseHandler;
    }

    @Override
    protected boolean onProcessingFailure(int baseMessageType, long correlationId) {
      switch (baseMessageType) {
        case POST_ORDER_MESSAGE_TYPE -> {
          return postOrderResponseHandler.onServerDecodeError(correlationId);
        }

        case CANCEL_ALL_MESSAGE_TYPE -> {
          return cancelAllResponseHandler.onServerDecodeError(correlationId);
        }

        default -> throw illegal("Unknown message type " + baseMessageType);
      }
    }

    @Override
    protected boolean onMessage(
        int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {

      switch (messageType) {
        case POST_ORDER_MESSAGE_TYPE -> {
          if (length != POST_ORDER_RESPONSE_SIZE) {
            postOrderResponseDecode.set(buffer, offset);
            return postOrderResponseHandler.onResponse(correlationId, postOrderResponseDecode);

          } else {
            return postOrderResponseHandler.onClientDecodeError(correlationId);
          }
        }

        case CANCEL_ALL_MESSAGE_TYPE -> {
          if (length != CANCEL_ALL_RESPONSE_SIZE) {
            cancelAllResponseDecode.set(buffer, offset);
            return cancelAllResponseHandler.onResponse(correlationId, cancelAllResponseDecode);

          } else {
            return cancelAllResponseHandler.onClientDecodeError(correlationId);
          }
        }

        default -> throw illegal("Unknown message type " + messageType);
      }
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
      throw illegal("claim failed");
    }

    @Override
    public void setQuoteAssetId(int quoteAssetId) {
      throw illegal("claim failed");
    }

    @Override
    public void setQuantityUnscaled(long quantityUnscaled) {
      throw illegal("claim failed");
    }

    @Override
    public void setQuantityScale(int quantityScale) {
      throw illegal("claim failed");
    }

    @Override
    public void setRateUnscaled(long rateUnscaled) {
      throw illegal("claim failed");
    }

    @Override
    public void setRateScale(int rateScale) {
      throw illegal("claim failed");
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
