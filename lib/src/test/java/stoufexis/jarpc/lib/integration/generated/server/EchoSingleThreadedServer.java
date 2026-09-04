package stoufexis.jarpc.lib.integration.generated.server;

import stoufexis.jarpc.lib.common.*;

import stoufexis.jarpc.lib.integration.generated.common.*;

public interface EchoSingleThreadedServer extends Poll {

  EchoResponseEncode claimEcho(long clientId, long correlationId, ClaimHandle claimHandle);

  interface EchoRequestHandler {
    boolean onRequest(long clientId, long correlationId, EchoRequestDecode t, EchoSingleThreadedServer server);
  }

}

