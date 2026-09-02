package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;

public interface AcquireRequestEncode {

  void setKey(long key);


  void setValue(Bytes value);




  default void set(AcquireRequestDecode decode) {

    setKey(decode.key());

    setValue(decode.value());


  }
}

