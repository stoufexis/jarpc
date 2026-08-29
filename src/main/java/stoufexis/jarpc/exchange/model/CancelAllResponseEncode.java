package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.model.Encode;

public interface CancelAllResponseEncode extends Encode {
  void setStatusCode(int statusCode);
}
