package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.model.Encode;

public interface PostOrderResponseEncode extends Encode {
  void setStatusCode(int statusCode);
}
