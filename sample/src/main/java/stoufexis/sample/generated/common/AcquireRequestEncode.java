package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface AcquireRequestEncode {

  void setKey(long key);


  void setValue(Bytes value);




  default void set(AcquireRequestDecode decode) {

    setKey(decode.key());

    setValue(decode.value());


  }
}

