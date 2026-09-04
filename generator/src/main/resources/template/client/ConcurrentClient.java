package _package_.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.concurrent.*;
import stoufexis.jarpc.lib.common.*;

import _package_.common.*;

public interface _Service_ConcurrentClient {
  /// foreachType
  boolean _type_(_Type_RequestDecode request, _Type_ResponseHandler response);

  interface _Type_ResponseHandler extends OnClientDecodeError {
    boolean onResponse(_Type_ResponseDecode t);
  }
  /// foreachType
}
