package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;

public class DecodeFailureResponse extends Message {
  private static final int STRING_SIZE = 50;
  private static final int MESSAGE_SIZE = STRING_SIZE + 4;

  private final ByteBuffer bytes = ByteBuffer.allocate(STRING_SIZE);
  private int bytesSize;
  private int baseMessageType;

  public DecodeFailureResponse() {
    super(DecodeFailureResponse.class, MESSAGE_SIZE);
  }

  public void reset() {
    uninitialize();
    bytesSize = 0;
    baseMessageType = 0;
  }

  public int getBaseMessageType() {
    return baseMessageType;
  }

  public int getBytesSize() {
    return bytesSize;
  }

  public ByteBuffer getBytes() {
    return bytes;
  }

  @Override
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    initialize();

    this.baseMessageType = buffer.getInt(offset);
    buffer.getBytes(offset + 4, bytes, bytesSize = length - 4);
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
    buffer.putInt(offset, baseMessageType);
    buffer.putBytes(offset + 4, bytes, bytesSize);
  }
}
