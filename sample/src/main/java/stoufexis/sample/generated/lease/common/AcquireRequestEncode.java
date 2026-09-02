package stoufexis.sample.generated.lease.common;

public interface AcquireRequestEncode {

  void setKey(long key);


  void setValue(byte[] value);


  void setTtlSeconds(int ttlSeconds);




  default void set(AcquireRequestDecode decode) {

    setKey(decode.getKey());

    setValue(decode.getValue());

    setTtlSeconds(decode.getTtlSeconds());


  }
}

