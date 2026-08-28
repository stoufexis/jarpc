package stoufexis.jarpc.exchange.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.Message;

public final class PostOrderRequest extends Message {
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
    offset += 4;
    this.quoteAssetId = buffer.getInt(offset);
    offset += 4;
    this.quantityUnscaled = buffer.getLong(offset);
    offset += 8;
    this.quantityScale = buffer.getInt(offset);
    offset += 4;
    this.rateUnscaled = buffer.getLong(offset);
    offset += 8;
    this.rateScale = buffer.getInt(offset);
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();

    buffer.putInt(offset, this.baseAssetId);
    offset += 4;
    buffer.putInt(offset, this.quoteAssetId);
    offset += 4;
    buffer.putLong(offset, this.quantityUnscaled);
    offset += 8;
    buffer.putInt(offset, this.quantityScale);
    offset += 4;
    buffer.putLong(offset, this.rateUnscaled);
    offset += 8;
    buffer.putInt(offset, this.rateScale);
  }

  @Override
  public String toString() {
    return "PostOrderRequest{initialized="
        + isInitialized()
        + ",baseAssetId="
        + baseAssetId
        + ",quoteAssetId="
        + quoteAssetId
        + ",quantityUnscaled="
        + quantityUnscaled
        + ",quantityScale="
        + quantityScale
        + ",rateUnscaled="
        + rateUnscaled
        + ",rateScale="
        + rateScale
        + "}";
  }
}
