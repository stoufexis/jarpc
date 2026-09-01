package _package_.server;

import _package_.common.*;

public interface _Service_ConcurrentServer {

  /// foreachType
  void register_Type_(_Type_ResponseHandler _type_Response);

  boolean _type_(long clientId, long correlationId, _Type_RequestDecode request);

  interface _Type_ResponseHandler {
    boolean onResponse(long clientId, long correlationId, _Type_ResponseDecode t);
  }

  /// foreachType
}
