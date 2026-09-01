package stoufexis.sample.generated.execution.common;

public interface CancelOrderResponseEncode {

  void setStatusCode(int statusCode);




  default void set(CancelOrderResponseDecode decode) {

    setStatusCode(decode.getStatusCode());


  }
}

