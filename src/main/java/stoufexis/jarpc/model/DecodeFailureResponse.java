package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;

public class DecodeFailureResponse extends Message {
  private static final int MESSAGE_SIZE = 50;

  private final ByteBuffer bytes = ByteBuffer.allocate(MESSAGE_SIZE);

  private int currentSize = 0;

  public DecodeFailureResponse() {
    super(DecodeFailureResponse.class, MESSAGE_SIZE);
  }

  public void reset() {
    uninitialize();
    currentSize = 0;
  }

  @Override
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    initialize();

    currentSize = length;
    buffer.getBytes(offset, bytes, length);
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
    buffer.putBytes(offset, bytes, currentSize);
  }
}
