package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;

public interface QueryRequestEncode {

  void setKey(long key);




  default void set(QueryRequestDecode decode) {

    setKey(decode.key());


  }
}

