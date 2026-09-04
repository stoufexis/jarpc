package stoufexis.jarpc.lib.integration.generated.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.concurrent.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.jarpc.lib.integration.generated.common.*;

public interface EchoConcurrentClient {

  boolean echo(EchoRequestDecode request, EchoResponseHandler response);

  interface EchoResponseHandler extends OnClientDecodeError {
    boolean onResponse(EchoResponseDecode t);
  }

}

