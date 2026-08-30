package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.common.PostOrderRequestDecode;

public interface PostOrderRequestHandler {
  boolean onRequest(long clientId, long correlationId, PostOrderRequestDecode t);
}
