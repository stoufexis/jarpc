package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import static stoufexis.jarpc.util.Util.illegal;

public final class MessageHeaderCodec {
  public static final int HEADER_SIZE = 12;

  private MessageHeaderCodec() {}

  public static void encode(
      MutableDirectBuffer buffer, int offset, long correlationId, int messageType) {

    buffer.putLong(offset, correlationId);
    buffer.putInt(offset + 8, messageType);
  }

  public static void assertSize(int length) {
    if (length < HEADER_SIZE) {
      throw illegal("Message does not contain a header");
    }
  }

  public static long decodeCorrelationId(DirectBuffer buffer, int offset) {
    return buffer.getLong(offset);
  }

  public static int decodeMessageType(DirectBuffer buffer, int offset) {
    return buffer.getInt(offset + 8);
  }
}
