package stoufexis.sample.generated.lease.common;

public interface QueryRequestEncode {

  void setKey(long key);




  default void set(QueryRequestDecode decode) {

    setKey(decode.getKey());


  }
}

