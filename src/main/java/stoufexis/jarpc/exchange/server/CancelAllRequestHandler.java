package stoufexis.jarpc.exchange.server;

import stoufexis.jarpc.exchange.common.CancelAllRequestDecode;

public interface CancelAllRequestHandler {
  boolean onRequest(long clientId, long correlationId, CancelAllRequestDecode t);
}
