package stoufexis.sample.generated.lease.common;

public interface QueryResponseEncode {

  void setExists(boolean exists);


  void setValue(byte[] value);


  void setExpiresInSeconds(int expiresInSeconds);




  default void set(QueryResponseDecode decode) {

    setExists(decode.getExists());

    setValue(decode.getValue());

    setExpiresInSeconds(decode.getExpiresInSeconds());


  }
}

