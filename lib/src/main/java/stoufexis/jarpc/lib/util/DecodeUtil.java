package stoufexis.jarpc.lib.util;

import org.agrona.DirectBuffer;

public abstract class DecodeUtil {
  protected BufferExt buffer = new BufferExt();

  public final void set(DirectBuffer buffer, int offset) {
    this.buffer.wrap(buffer, offset);
  }

  public static final class BufferExt {
    private DirectBuffer buffer;
    private int offset;
    private byte[] bytes16;
    private byte[] bytes32;
    private byte[] bytes64;
    private byte[] bytes128;

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

    public byte[] getBytes16(int index) {
      if (bytes16 == null) bytes16 = new byte[16];
      buffer.getBytes(offset + index, bytes16);
      return bytes16;
    }

    public byte[] getBytes32(int index) {
      if (bytes32 == null) bytes32 = new byte[32];
      buffer.getBytes(offset + index, bytes32);
      return bytes32;
    }

    public byte[] getBytes64(int index) {
      if (bytes64 == null) bytes64 = new byte[64];
      buffer.getBytes(offset + index, bytes64);
      return bytes64;
    }

    public byte[] getBytes128(int index) {
      if (bytes128 == null) bytes128 = new byte[128];
      buffer.getBytes(offset + index, bytes128);
      return bytes128;
    }
  }
}
