package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;

public interface AcquireResponseEncode {

  void setAcquired(boolean acquired);




  default void set(AcquireResponseDecode decode) {

    setAcquired(decode.acquired());


  }
}

