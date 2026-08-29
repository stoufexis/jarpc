package stoufexis.jarpc.model;

import org.agrona.MutableDirectBuffer;

public final class MessageHeaderEncode {

  public static void encode(
      MutableDirectBuffer buffer, int offset, long correlationId, int messageType) {

    buffer.putLong(offset, correlationId);
    buffer.putInt(offset + 4, messageType);
  }
}
