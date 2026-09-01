package stoufexis.sample.generated.execution.common;

public interface PostLimitOrderResponseEncode {

  void setGeneratedId(long generatedId);


  void setStatusCode(int statusCode);




  default void set(PostLimitOrderResponseDecode decode) {

    setGeneratedId(decode.getGeneratedId());

    setStatusCode(decode.getStatusCode());


  }
}

