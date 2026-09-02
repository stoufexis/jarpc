package stoufexis.jarpc.lib.util;

import org.agrona.DirectBuffer;

public abstract class DecodeUtil {
  protected BufferExt buffer = new BufferExt();
  protected int offset;

  public final void set(DirectBuffer buffer, int offset) {
    this.buffer.wrap(buffer, offset);
  }

  public static final class BufferExt {
    private DirectBuffer buffer;
    private int offset;

    private void wrap(DirectBuffer buffer, int offset) {
      this.buffer = buffer;
      this.offset = offset;
    }

    public boolean getBoolean(int index) {
      return buffer.getByte(offset + index) == 1;
    }

    public long getLong(int index) {
      return buffer.getLong(offset + index);
    }

    public int getInt(int index) {
      return buffer.getInt(offset + index);
    }

    public double getDouble(int index) {
      return buffer.getDouble(offset + index);
    }

    public float getFloat(int index) {
      return buffer.getFloat(offset + index);
    }

    public short getShort(int index) {
      return buffer.getShort(offset + index);
    }

    public byte getByte(int index) {
      return buffer.getByte(offset + index);
    }

    public byte[] getBytes16(int index, byte[] src) {
      buffer.getBytes(offset + index, src, 0, 16);
      return src;
    }

    public byte[] getBytes32(int index, byte[] src) {
      buffer.getBytes(offset + index, src, 0, 32);
      return src;
    }

    public byte[] getBytes64(int index, byte[] src) {
      buffer.getBytes(offset + index, src, 0, 64);
      return src;
    }

    public byte[] getBytes128(int index, byte[] src) {
      buffer.getBytes(offset + index, src, 0, 128);
      return src;
    }
  }
}
