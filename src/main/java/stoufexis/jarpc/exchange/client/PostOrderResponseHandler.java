package stoufexis.jarpc.exchange.client;

import stoufexis.jarpc.client.ClientHandler;
import stoufexis.jarpc.exchange.model.PostOrderResponseDecode;

public interface PostOrderResponseHandler extends ClientHandler {
  boolean onResponse(long correlationId, PostOrderResponseDecode t);
}
