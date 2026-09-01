package stoufexis.sample.generated.updates.server;

import stoufexis.jarpc.lib.model.*;

import stoufexis.sample.generated.updates.common.*;

public interface UpdatesSingleThreadedServer extends Poll {


  SubscribeOrderUpdatesResponseEncode claimSubscribeOrderUpdates(long clientId, long correlationId, ClaimHandle claimHandle);

  interface SubscribeOrderUpdatesRequestHandler {
    boolean onRequest(long clientId, long correlationId, SubscribeOrderUpdatesRequestDecode t);
  }


}

