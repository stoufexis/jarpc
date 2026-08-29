package stoufexis.jarpc.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import static stoufexis.jarpc.util.Util.illegal;

public final class ProcessingFailureCodec {
  public static final int MESSAGE_TYPE_ID = -1;
  public static final int PROCESSING_FAILURE_SIZE = 4;

  public static void assertSize(int length) {
    if (length < PROCESSING_FAILURE_SIZE) {
      throw illegal("Could not decode processing failure. Message too small.");
    }
  }

  public static void encode(MutableDirectBuffer buffer, int offset, int baseMessageType) {
    buffer.putInt(offset, baseMessageType);
  }

  public static int decodeBaseMessageType(DirectBuffer buffer, int offset) {
    return buffer.getInt(offset);
  }
}
