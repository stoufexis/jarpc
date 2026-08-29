package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.exchange.model.CancelAllRequestEncode;
import stoufexis.jarpc.exchange.model.Catalog;
import stoufexis.jarpc.exchange.model.PostOrderRequestEncode;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.model.MessageHeader;
import stoufexis.jarpc.model.MessageHeaderEncode;
import stoufexis.jarpc.util.EncodeFailUtil;
import stoufexis.jarpc.util.EncodeSuccessUtil;

import static stoufexis.jarpc.util.Util.illegal;
import static stoufexis.jarpc.util.Util.interpretErrorCode;

/** Meant to be used by a single thread. */
public class JarpcExchangeClientLow implements ExchangeClientLow {
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

  private final Publication publication;
  private final Subscription subscription;

  public JarpcExchangeClientLow(Publication publication, Subscription subscription) {
    this.publication = publication;
    this.subscription = subscription;
  }

  @Override
  public PostOrderRequestEncode claimPostOrder() {
    long result =
        publication.tryClaim(Catalog.postOrder.requestSize() + MessageHeader.HEADER_SIZE, claim);

    if (result < 0) {
      postOrderRequestEncodeFail.setErrorCode(interpretErrorCode(result));
      return postOrderRequestEncodeFail;
    }

    long id = correlationId++;
    int offset = claim.offset();
    MutableDirectBuffer buffer = claim.buffer();
    MessageHeaderEncode.encode(buffer, offset, id, Catalog.postOrder.messageTypeId());
    postOrderRequestEncodeSuccess.setSuccess(id, buffer, offset + MessageHeader.HEADER_SIZE, claim);

    return postOrderRequestEncodeSuccess;
  }

  @Override
  public CancelAllRequestEncode claimCancelAll() {
    long result =
        publication.tryClaim(Catalog.cancelAll.requestSize() + MessageHeader.HEADER_SIZE, claim);

    if (result < 0) {
      cancelAllRequestEncodeFail.setErrorCode(interpretErrorCode(result));
      return cancelAllRequestEncodeFail;
    }

    long id = correlationId++;
    int offset = claim.offset();
    MutableDirectBuffer buffer = claim.buffer();
    MessageHeaderEncode.encode(buffer, offset, id, Catalog.cancelAll.messageTypeId());
    cancelAllRequestEncodeSuccess.setSuccess(id, buffer, offset + MessageHeader.HEADER_SIZE, claim);

    return cancelAllRequestEncodeSuccess;
  }

  @Override
  public int poll(
      PostOrderResponseHandler postOrderHandler, CancelAllResponseHandler cancelAllCallback) {
    return 0;
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
}
