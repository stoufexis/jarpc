package stoufexis.jarpc.exchange.common;

public interface CancelAllResponseEncode {
  void setStatusCode(int statusCode);

  default void set(CancelAllResponseDecode decode) {
    setStatusCode(decode.getStatusCode());
  }
}
