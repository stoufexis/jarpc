package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public final class MessageHeader {
  public static final int HEADER_SIZE = 13;

  private boolean initialized;
  private long correlationId;
  private int messageType;
  private boolean last;

  public void reset() {
    initialized = false;
    correlationId = 0;
    messageType = 0;
    last = false;
  }

  public long getCorrelationId() {
    return correlationId;
  }

  public int getMessageType() {
    return messageType;
  }

  public boolean isLast() {
    return last;
  }

  public void set(long correlationId, int messageType, boolean last) {
    initialized = true;
    this.correlationId = correlationId;
    this.messageType = messageType;
    this.last = last;
  }

  /**
   * @throws IllegalArgumentException if the header cannot be decoded.
   */
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    this.initialized = true;
    this.correlationId = buffer.getLong(offset);
    this.messageType = buffer.getInt(offset + 8);
    this.last = buffer.getByte(offset + 12) != 0;
  }

  /**
   * @throws IllegalStateException if the header cannot be decoded.
   */
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
    buffer.putLong(offset, correlationId);
    buffer.putInt(offset + 8, messageType);
    buffer.putByte(offset + 12, (byte) (last ? 1 : 0));
  }

  private void checkInitialized() {
    if (!initialized) {
      throw new IllegalStateException("Header cannot be encoded. It is not initialized.");
    }
  }

  private static void checkSize(int length) {
    if (length < HEADER_SIZE) {
      throw new IllegalArgumentException(
          String.format("Header cannot be decoded, message size is too small; %d", length));
    }
  }
}
