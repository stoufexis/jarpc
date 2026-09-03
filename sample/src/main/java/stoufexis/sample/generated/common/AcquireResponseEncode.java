package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface AcquireResponseEncode {

  void setAcquired(boolean acquired);




  default void set(AcquireResponseDecode decode) {

    setAcquired(decode.acquired());


  }
}

