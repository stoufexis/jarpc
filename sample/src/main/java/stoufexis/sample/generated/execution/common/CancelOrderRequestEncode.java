package stoufexis.sample.generated.execution.common;

public interface CancelOrderRequestEncode {

  void setGeneratedId(long generatedId);




  default void set(CancelOrderRequestDecode decode) {

    setGeneratedId(decode.getGeneratedId());


  }
}

