package stoufexis.sample.generated.updates.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import stoufexis.sample.generated.updates.common.*;

public interface UpdatesSingleThreadedClient extends Poll {


  SubscribeOrderUpdatesRequestEncode claimSubscribeOrderUpdates(ClaimHandle claimHandle);

  interface SubscribeOrderUpdatesResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, SubscribeOrderUpdatesResponseDecode t);
  }


}

