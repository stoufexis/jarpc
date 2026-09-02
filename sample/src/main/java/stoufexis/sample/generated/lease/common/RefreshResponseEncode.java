package stoufexis.sample.generated.lease.common;

public interface RefreshResponseEncode {

  void setAcquired(boolean acquired);




  default void set(RefreshResponseDecode decode) {

    setAcquired(decode.getAcquired());


  }
}

