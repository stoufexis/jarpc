package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public final class DecodeFailureResponse extends Message {
  private static final int MESSAGE_SIZE = 4;

  private int baseMessageType;

  public DecodeFailureResponse() {
    super(DecodeFailureResponse.class, MESSAGE_SIZE);
  }

  public void reset() {
    uninitialize();
    baseMessageType = 0;
  }

  public void set(int baseMessageType) {
    initialize();
    this.baseMessageType = baseMessageType;
  }

  public int getBaseMessageType() {
    return baseMessageType;
  }

  @Override
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    initialize();

    this.baseMessageType = buffer.getInt(offset);
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
    buffer.putInt(offset, baseMessageType);
  }
}
