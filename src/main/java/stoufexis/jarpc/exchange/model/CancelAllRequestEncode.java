package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.model.Encode;

public interface CancelAllRequestEncode extends Encode {
  default void set(CancelAllRequestDecode decode) {}
}
