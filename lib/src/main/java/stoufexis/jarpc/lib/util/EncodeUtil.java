package stoufexis.jarpc.lib.util;

import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.lib.model.Bytes;

public abstract class EncodeUtil {
  protected BufferExt buffer = new BufferExt();

  public final void set(MutableDirectBuffer buffer, int offset) {
    this.buffer.wrap(buffer, offset);
  }

  public static final class BufferExt {
    private MutableDirectBuffer buffer;
    private int offset;

    private void wrap(MutableDirectBuffer buffer, int offset) {
      this.buffer = buffer;
      this.offset = offset;
    }

    public void putBoolean(int index, boolean value) {
      buffer.putByte(offset + index, (byte) (value ? 1 : 0));
    }

    public void putLong(int index, long value) {
      buffer.putLong(offset + index, value);
    }

    public void putInt(int index, int value) {
      buffer.putInt(offset + index, value);
    }

    public void putDouble(int index, double value) {
      buffer.putDouble(offset + index, value);
    }

    public void putFloat(int index, float value) {
      buffer.putFloat(offset + index, value);
    }

    public void putShort(int index, short value) {
      buffer.putShort(offset + index, value);
    }

    public void putByte(int index, byte value) {
      buffer.putByte(offset + index, value);
    }

    public void putBytes16(int index, Bytes src) {
      buffer.putBytes(offset + index, src.getArray(), 0, 16);
    }

    public void putBytes32(int index, Bytes src) {
      buffer.putBytes(offset + index, src.getArray(), 0, 32);
    }

    public void putBytes64(int index, Bytes src) {
      buffer.putBytes(offset + index, src.getArray(), 0, 64);
    }

    public void putBytes128(int index, Bytes src) {
      buffer.putBytes(offset + index, src.getArray(), 0, 128);
    }
  }
}
