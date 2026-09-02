package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;

public interface RefreshResponseEncode {

  void setAcquired(boolean acquired);




  default void set(RefreshResponseDecode decode) {

    setAcquired(decode.acquired());


  }
}

