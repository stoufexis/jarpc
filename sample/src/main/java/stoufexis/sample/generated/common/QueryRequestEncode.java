package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface QueryRequestEncode {

  void setKey(long key);


  default void set(QueryRequestDecode decode) {

    setKey(decode.key());


  }
}

