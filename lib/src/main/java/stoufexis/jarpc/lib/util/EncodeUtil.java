package stoufexis.jarpc.lib.util;

import org.agrona.MutableDirectBuffer;

public abstract class EncodeUtil {
  protected MutableDirectBuffer buffer;
  protected int offset;

  public final void set(MutableDirectBuffer buffer, int offset) {
    this.buffer = buffer;
    this.offset = offset;
  }
}
