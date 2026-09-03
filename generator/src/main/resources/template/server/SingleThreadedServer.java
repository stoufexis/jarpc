package _package_.server;

import stoufexis.jarpc.lib.common.*;

import _package_.common.*;

public interface _Service_SingleThreadedServer extends Poll {

  /// foreachType
  _Type_ResponseEncode claim_Type_(long clientId, long correlationId, ClaimHandle claimHandle);

  interface _Type_RequestHandler {
    boolean onRequest(long clientId, long correlationId, _Type_RequestDecode t, _Service_SingleThreadedServer server);
  }

  /// foreachType
}
