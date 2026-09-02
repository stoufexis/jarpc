package stoufexis.jarpc.lib.util;

import org.agrona.DirectBuffer;
import stoufexis.jarpc.lib.model.Bytes;

public abstract class DecodeUtil {
  protected BufferExt buffer = new BufferExt();

  public final void set(DirectBuffer buffer, int offset) {
    this.buffer.wrap(buffer, offset);
  }

  public static final class BufferExt {
    private DirectBuffer buffer;
    private int offset;
    private Bytes bytes16;
    private Bytes bytes32;
    private Bytes bytes64;
    private Bytes bytes128;

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

    public Bytes getBytes16(int index) {
      if (bytes16 == null) bytes16 = new Bytes(16);
      buffer.getBytes(offset + index, bytes16.getArray());
      return bytes16;
    }

    public Bytes getBytes32(int index) {
      if (bytes32 == null) bytes32 = new Bytes(32);
      buffer.getBytes(offset + index, bytes32.getArray());
      return bytes32;
    }

    public Bytes getBytes64(int index) {
      if (bytes64 == null) bytes64 = new Bytes(64);
      buffer.getBytes(offset + index, bytes64.getArray());
      return bytes64;
    }

    public Bytes getBytes128(int index) {
      if (bytes128 == null) bytes128 = new Bytes(128);
      buffer.getBytes(offset + index, bytes128.getArray());
      return bytes128;
    }
  }
}
