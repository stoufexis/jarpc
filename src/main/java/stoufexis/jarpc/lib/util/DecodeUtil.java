package stoufexis.jarpc.lib.util;

import org.agrona.DirectBuffer;

public abstract class DecodeUtil {
  protected DirectBuffer buffer;
  protected int offset;

  public final void set(DirectBuffer buffer, int offset) {
    this.buffer = buffer;
    this.offset = offset;
  }
}
