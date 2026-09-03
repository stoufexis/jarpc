package stoufexis.jarpc.lib.model;

/**
 * Instances of Bytes must be copied immediately upon reading, as they will be re-used internally
 */
public class Bytes {
  private final int size;
  private byte[] bytes;

  public Bytes(int size) {
    this.size = size;
    this.bytes = new byte[size];
  }

  public byte[] getArray() {
    return bytes;
  }

  public void copy(Bytes that) {
    if (this.size != that.size)
      throw new IllegalArgumentException("Cannot copy Bytes of different sizes");
    System.arraycopy(that.bytes, 0, this.bytes, 0, this.size);
  }

  @Override
  public String toString() {
    return new String(bytes);
  }
}
