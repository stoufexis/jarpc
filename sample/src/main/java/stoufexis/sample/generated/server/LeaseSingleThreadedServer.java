package stoufexis.sample.generated.server;

import stoufexis.jarpc.lib.common.*;

import stoufexis.sample.generated.common.*;

public interface LeaseSingleThreadedServer extends Poll {

  AcquireResponseEncode claimAcquire(long clientId, long correlationId, ClaimHandle claimHandle);

  interface AcquireRequestHandler {
    boolean onRequest(long clientId, long correlationId, AcquireRequestDecode t, LeaseSingleThreadedServer server);
  }
  RefreshResponseEncode claimRefresh(long clientId, long correlationId, ClaimHandle claimHandle);

  interface RefreshRequestHandler {
    boolean onRequest(long clientId, long correlationId, RefreshRequestDecode t, LeaseSingleThreadedServer server);
  }
  QueryResponseEncode claimQuery(long clientId, long correlationId, ClaimHandle claimHandle);

  interface QueryRequestHandler {
    boolean onRequest(long clientId, long correlationId, QueryRequestDecode t, LeaseSingleThreadedServer server);
  }

}

