package _package_.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import _package_.common.*;

public interface _Service_SingleThreadedClient extends Poll {

  /// foreachType
  _Type_RequestEncode claim_Type_(ClaimHandle claimHandle);

  interface _Type_ResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, _Type_ResponseDecode t);
  }

  /// foreachType
}
