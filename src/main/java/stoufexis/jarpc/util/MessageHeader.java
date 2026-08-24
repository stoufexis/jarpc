package stoufexis.jarpc.util;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class MessageHeader {
  public static final int HEADER_SIZE = 12;

  private boolean initialized;
  private long correlationId;
  private int messageType;

  public void reset() {
    initialized = false;
    correlationId = 0;
    messageType = 0;
  }

  public long getCorrelationId() {
    return correlationId;
  }

  public int getMessageType() {
    return messageType;
  }

  public void set(long correlationId, int messageType) {
    initialized = true;
    this.correlationId = correlationId;
    this.messageType = messageType;
  }

  /**
   * @throws IllegalArgumentException if the header cannot be decoded.
   */
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    this.initialized = true;
    this.correlationId = buffer.getLong(offset);
    this.messageType = buffer.getInt(offset + 8);
  }

  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
    buffer.putLong(offset, correlationId);
    buffer.putInt(offset + 8, messageType);
  }

  private void checkInitialized() {
    if (!initialized) {
      throw new IllegalArgumentException("Header cannot be encoded. It is not initialized.");
    }
  }

  private static void checkSize(int length) {
    if (length < HEADER_SIZE) {
      throw new IllegalArgumentException(
          String.format("Header cannot be decoded, message size is too small; %d", length));
    }
  }
}
