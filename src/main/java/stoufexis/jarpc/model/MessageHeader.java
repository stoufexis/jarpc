package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public final class MessageHeader {
  public static final int HEADER_SIZE = 9;

  private boolean initialized;
  private int correlationId;
  private int messageType;
  private boolean last;

  public void reset() {
    initialized = false;
    correlationId = 0;
    messageType = 0;
    last = false;
  }

  public int getCorrelationId() {
    return correlationId;
  }

  public int getMessageType() {
    return messageType;
  }

  public boolean isLast() {
    return last;
  }

  public void set(int correlationId, int messageType, boolean last) {
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
    this.correlationId = buffer.getInt(offset);
    this.messageType = buffer.getInt(offset + 4);
    this.last = buffer.getByte(offset + 8) != 0;
  }

  /**
   * @throws IllegalStateException if the header cannot be decoded.
   */
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
    // FIXME correlationId is an int, but this writes 8 bytes. On little-endian the putInt
    //  below happens to overwrite exactly the sign-extension bytes, so it works by accident.
    //  On big-endian the correlationId lands in bytes 4-7 and is destroyed by that putInt, so
    //  every correlationId decodes as 0. Should be putInt.
    buffer.putLong(offset, correlationId);
    buffer.putInt(offset + 4, messageType);
    buffer.putByte(offset + 8, (byte) (last ? 1 : 0));
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
