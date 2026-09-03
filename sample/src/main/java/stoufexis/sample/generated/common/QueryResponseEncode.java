package stoufexis.sample.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface QueryResponseEncode {

  void setExists(boolean exists);


  void setValue(Bytes value);


  void setExpiresInSeconds(int expiresInSeconds);




  default void set(QueryResponseDecode decode) {

    setExists(decode.exists());

    setValue(decode.value());

    setExpiresInSeconds(decode.expiresInSeconds());


  }
}

