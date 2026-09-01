package _package_.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import _package_.common.*;

public interface SingleThreaded_Service_Client extends Poll {

  /// foreachType
  _type_RequestEncode claim_Type_(ClaimHandle claimHandle);

  interface _Type_ResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, _Type_ResponseDecode t);
  }

  /// foreachType
}
