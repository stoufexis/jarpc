package stoufexis.jarpc;

import org.agrona.MutableDirectBuffer;

public interface Decoder<T> {
  T decode(int offset, int length, MutableDirectBuffer b);
}
