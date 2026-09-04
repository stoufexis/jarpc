package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface RefreshRequestEncode {

  void setKey(long key);


  default void set(RefreshRequestDecode decode) {

    setKey(decode.key());


  }
}

