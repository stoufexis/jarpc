package stoufexis.jarpc.tmp;

import org.agrona.MutableDirectBuffer;

public interface Encoder<T> {
  int length(T t);

  void encode(int offset, T t, MutableDirectBuffer b);
}
