package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import stoufexis.jarpc.exchange.model.CancelAllRequestEncode;
import stoufexis.jarpc.exchange.model.PostOrderRequestEncode;
import stoufexis.jarpc.model.MessageHeader;

/** Meant to be used by a single thread. */
public class JarpcExchangeClientLow implements ExchangeClientLow {
  private final MessageHeader header = new MessageHeader();

  private final Publication publication;
  private final Subscription subscription;

  public JarpcExchangeClientLow(Publication publication, Subscription subscription) {
    this.publication = publication;
    this.subscription = subscription;
  }

  @Override
  public PostOrderRequestEncode claimPostOrder() {
    publication.tryClaim()
    return null;
  }

  @Override
  public CancelAllRequestEncode claimCancelAll() {
    return null;
  }

  @Override
  public int poll(
      PostOrderResponseHandler postOrderHandler, CancelAllResponseHandler cancelAllCallback) {
    return 0;
  }

  private class PostOrderRequestEncodeImpl implements PostOrderRequestEncode {
    final BufferClaim claim = new BufferClaim();

    private long correlationId = 0;

    @Override
    public void setBaseAssetId(int baseAssetId) {

    }

    @Override
    public void setQuoteAssetId(int quoteAssetId) {

    }

    @Override
    public void setQuantityUnscaled(long quantityUnscaled) {

    }

    @Override
    public void setQuantityScale(int quantityScale) {

    }

    @Override
    public void setRateUnscaled(long rateUnscaled) {

    }

    @Override
    public void setRateScale(int rateScale) {

    }

    @Override
    public int claimResult() {
      return 0;
    }

    @Override
    public void commit() {

    }

    @Override
    public void abort() {

    }
  }
}
