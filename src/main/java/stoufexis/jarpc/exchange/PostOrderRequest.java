package stoufexis.jarpc.exchange;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.Message;

public class PostOrderRequest extends Message {
  private int baseAssetId;
  private int quoteAssetId;
  private long quantityUnscaled;
  private int quantityScale;
  private long rateUnscaled;
  private int rateScale;

  public PostOrderRequest() {
    super(PostOrderRequest.class, 32);
  }

  public void reset() {
    uninitialize();
    baseAssetId = 0;
    quoteAssetId = 0;
    quantityUnscaled = 0;
    quantityScale = 0;
    rateUnscaled = 0;
    rateScale = 0;
  }

  public int getBaseAsset() {
    return baseAssetId;
  }

  public int quoteAsset() {
    return quoteAssetId;
  }

  public long getQuantityUnscaled() {
    return quantityUnscaled;
  }

  public int getQuantityScale() {
    return quantityScale;
  }

  public long getRateUnscaled() {
    return rateUnscaled;
  }

  public int getRateScale() {
    return rateScale;
  }

  public void set(
      long correlationId,
      int baseAssetId,
      int quoteAssetId,
      long quantityUnscaled,
      int quantityScale,
      long rateUnscaled,
      int rateScale) {
    initialize();
    this.baseAssetId = baseAssetId;
    this.quoteAssetId = quoteAssetId;
    this.quantityUnscaled = quantityUnscaled;
    this.quantityScale = quantityScale;
    this.rateUnscaled = rateUnscaled;
    this.rateScale = rateScale;
  }

  @Override
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    initialize();

    this.baseAssetId = buffer.getInt(offset);
    this.quoteAssetId = buffer.getInt(offset + 4);
    this.quantityUnscaled = buffer.getLong(offset + 8);
    this.quantityScale = buffer.getInt(offset + 16);
    this.rateUnscaled = buffer.getLong(offset + 20);
    this.rateScale = buffer.getInt(offset + 28);
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();

    buffer.putInt(offset, this.baseAssetId);
    buffer.putInt(offset + 4, this.quoteAssetId);
    buffer.putLong(offset + 8, this.quantityUnscaled);
    buffer.putInt(offset + 16, this.quantityScale);
    buffer.putLong(offset + 20, this.rateUnscaled);
    buffer.putInt(offset + 28, this.rateScale);
  }
}
