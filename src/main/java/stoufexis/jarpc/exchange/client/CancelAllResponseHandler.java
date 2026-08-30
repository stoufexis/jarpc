package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientHandler;
import stoufexis.jarpc.exchange.common.CancelAllResponseDecode;

public interface CancelAllResponseHandler extends ClientHandler {
  boolean onResponse(long correlationId, CancelAllResponseDecode t);
}
