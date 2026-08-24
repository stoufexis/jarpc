package stoufexis.jarpc;

import org.agrona.DirectBuffer;

public interface Decoder<T> {
  T decode(int offset, int length, DirectBuffer b);
}
