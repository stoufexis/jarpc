package stoufexis.jarpc.lib.common;

import org.agrona.DirectBuffer;

public abstract class DecodeUtil {
  private DirectBuffer buffer;
  private int offset;
  private Bytes bytes16;
  private Bytes bytes32;
  private Bytes bytes64;
  private Bytes bytes128;

  public final void set(DirectBuffer buffer, int offset) {
    this.buffer = buffer;
    this.offset = offset;
  }

  public final void set(ClaimHandle handle, int offset) {
    set(handle.getClaim().buffer(), offset);
  }

  protected boolean getBoolean(int index) {
    return buffer.getByte(offset + index) == 1;
  }

  protected long getLong(int index) {
    return buffer.getLong(offset + index);
  }

  protected int getInt(int index) {
    return buffer.getInt(offset + index);
  }

  protected double getDouble(int index) {
    return buffer.getDouble(offset + index);
  }

  protected float getFloat(int index) {
    return buffer.getFloat(offset + index);
  }

  protected short getShort(int index) {
    return buffer.getShort(offset + index);
  }

  protected byte getByte(int index) {
    return buffer.getByte(offset + index);
  }

  protected Bytes getBytes16(int index) {
    if (bytes16 == null) bytes16 = new Bytes(16);
    buffer.getBytes(offset + index, bytes16.backingArray());
    return bytes16;
  }

  protected Bytes getBytes32(int index) {
    if (bytes32 == null) bytes32 = new Bytes(32);
    buffer.getBytes(offset + index, bytes32.backingArray());
    return bytes32;
  }

  protected Bytes getBytes64(int index) {
    if (bytes64 == null) bytes64 = new Bytes(64);
    buffer.getBytes(offset + index, bytes64.backingArray());
    return bytes64;
  }

  protected Bytes getBytes128(int index) {
    if (bytes128 == null) bytes128 = new Bytes(128);
    buffer.getBytes(offset + index, bytes128.backingArray());
    return bytes128;
  }
}
