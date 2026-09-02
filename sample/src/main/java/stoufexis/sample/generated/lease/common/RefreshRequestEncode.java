package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;

public interface RefreshRequestEncode {

  void setKey(long key);




  default void set(RefreshRequestDecode decode) {

    setKey(decode.key());


  }
}

