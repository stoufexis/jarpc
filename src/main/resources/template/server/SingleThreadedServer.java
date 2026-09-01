package _package_.server;

import stoufexis.jarpc.lib.model.*;

import _package_.common.*;

public interface SingleThreadedExchangeServer extends Poll {

  /// foreachType
  _Type_ResponseEncode claim_Type_(long clientId, long correlationId, ClaimHandle claimHandle);

  interface _Type_RequestHandler {
    boolean onRequest(long clientId, long correlationId, _Type_RequestDecode t);
  }

  /// foreachType
}
