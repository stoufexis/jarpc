package stoufexis.sample.generated.lease.common;

public interface AcquireResponseEncode {

  void setAcquired(boolean acquired);




  default void set(AcquireResponseDecode decode) {

    setAcquired(decode.getAcquired());


  }
}

