package _package_.client;

import stoufexis.jarpc.lib.client.*;

import _package_.common.*;

public interface _Service_ConcurrentClient {
  /// foreachType
  boolean _type_(_Type_RequestDecode request, _Type_ResponseHandler response);

  interface _Type_ResponseHandler extends ClientHandler {
    boolean onResponse(_Type_ResponseDecode t);
  }

  /// foreachType
}
