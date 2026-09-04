package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface RefreshResponseEncode {

  void setAcquired(boolean acquired);



  default void set(RefreshResponseDecode decode) {

    setAcquired(decode.acquired());


  }
}

