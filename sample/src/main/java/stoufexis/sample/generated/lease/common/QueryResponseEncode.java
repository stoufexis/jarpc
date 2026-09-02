package stoufexis.sample.generated.lease.common;

import stoufexis.jarpc.lib.model.*;

public interface QueryResponseEncode {

  void setExists(boolean exists);


  void setValue(Bytes value);


  void setExpiresInSeconds(int expiresInSeconds);




  default void set(QueryResponseDecode decode) {

    setExists(decode.getExists());

    setValue(decode.getValue());

    setExpiresInSeconds(decode.getExpiresInSeconds());


  }
}

