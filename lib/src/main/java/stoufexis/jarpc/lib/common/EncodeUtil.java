package stoufexis.jarpc.lib.common;

import org.agrona.MutableDirectBuffer;

public abstract class EncodeUtil {
  private MutableDirectBuffer buffer;
  private int offset;

  public final void set(MutableDirectBuffer buffer, int offset) {
    this.buffer = buffer;
    this.offset = offset;
  }

  public final void set(ClaimHandle handle, int offset) {
    set(handle.getClaim().buffer(), offset);
  }

  protected void putBoolean(int index, boolean value) {
    buffer.putByte(offset + index, (byte) (value ? 1 : 0));
  }

  protected void putLong(int index, long value) {
    buffer.putLong(offset + index, value);
  }

  protected void putInt(int index, int value) {
    buffer.putInt(offset + index, value);
  }

  protected void putDouble(int index, double value) {
    buffer.putDouble(offset + index, value);
  }

  protected void putFloat(int index, float value) {
    buffer.putFloat(offset + index, value);
  }

  protected void putShort(int index, short value) {
    buffer.putShort(offset + index, value);
  }

  protected void putByte(int index, byte value) {
    buffer.putByte(offset + index, value);
  }

  protected void putBytes16(int index, Bytes src) {
    buffer.putBytes(offset + index, src.backingArray(), 0, 16);
  }

  protected void putBytes32(int index, Bytes src) {
    buffer.putBytes(offset + index, src.backingArray(), 0, 32);
  }

  protected void putBytes64(int index, Bytes src) {
    buffer.putBytes(offset + index, src.backingArray(), 0, 64);
  }
}
