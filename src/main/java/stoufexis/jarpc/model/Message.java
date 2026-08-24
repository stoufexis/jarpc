package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public abstract class Message {
  private boolean initialized;
  private final Class<?> clazz;
  private final int messageSize;

  public Message(Class<?> clazz, int messageSize) {
    this.clazz = clazz;
    this.messageSize = messageSize;
  }

  /**
   * @throws IllegalArgumentException if the message cannot be decoded as this type.
   */
  public abstract void decode(DirectBuffer buffer, int offset, int length);

  /**
   * @throws IllegalStateException if the message cannot be encoded as this type.
   */
  public abstract void encode(MutableDirectBuffer buffer, int offset);

  public final void uninitialize() {
    initialized = false;
  }

  public final void initialize() {
    initialized = true;
  }

  public final boolean isInitialized() {
    return initialized;
  }

  /** Max encoded size of this message type */
  public final int messageSize() {
    return messageSize;
  }

  /**
   * @throws IllegalArgumentException if the length exceeds the expected size
   */
  public final void checkSize(int length) {
    if (length > messageSize) {
      throw new IllegalArgumentException(
          String.format(
              "%s cannot be decoded. Expected size %d, received size %d",
              clazz.getName(), messageSize, length));
    }
  }

  /**
   * @throws IllegalStateException if the flag is false
   */
  public final void checkInitialized() {
    if (!initialized) {
      throw new IllegalArgumentException(
          String.format("%s cannot be encoded. It is not initialized.", clazz.getName()));
    }
  }
}
