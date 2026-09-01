package stoufexis.sample.generated.updates.server;

import stoufexis.sample.generated.updates.common.*;
import stoufexis.jarpc.lib.model.ClientHook;

public interface UpdatesConcurrentServer extends ClientHook {


  void registerSubscribeOrderUpdates(SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesResponse);

  boolean subscribeOrderUpdates(long clientId, long correlationId, SubscribeOrderUpdatesRequestDecode request);

  interface SubscribeOrderUpdatesResponseHandler {
    boolean onResponse(long clientId, long correlationId, SubscribeOrderUpdatesResponseDecode t);
  }


}

