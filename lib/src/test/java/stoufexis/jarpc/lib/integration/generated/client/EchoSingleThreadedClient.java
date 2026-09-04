package stoufexis.jarpc.lib.integration.generated.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.jarpc.lib.integration.generated.common.*;

public interface EchoSingleThreadedClient extends Poll {

  EchoRequestEncode claimEcho(ClaimHandle claimHandle);

  interface EchoResponseHandler extends OnClientDecodeError {
    boolean onResponse(long correlationId, EchoResponseDecode t);
  }

}

