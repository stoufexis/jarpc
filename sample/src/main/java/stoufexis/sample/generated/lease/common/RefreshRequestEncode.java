package stoufexis.sample.generated.lease.common;

public interface RefreshRequestEncode {

  void setKey(long key);




  default void set(RefreshRequestDecode decode) {

    setKey(decode.getKey());


  }
}

