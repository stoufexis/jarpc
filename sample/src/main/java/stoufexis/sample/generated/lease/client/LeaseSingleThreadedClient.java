package stoufexis.sample.generated.lease.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import stoufexis.sample.generated.lease.common.*;

public interface LeaseSingleThreadedClient extends Poll {


  AcquireRequestEncode claimAcquire(ClaimHandle claimHandle);

  interface AcquireResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, AcquireResponseDecode t);
  }

  RefreshRequestEncode claimRefresh(ClaimHandle claimHandle);

  interface RefreshResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, RefreshResponseDecode t);
  }

  QueryRequestEncode claimQuery(ClaimHandle claimHandle);

  interface QueryResponseHandler extends ClientHandler {
    boolean onResponse(long correlationId, QueryResponseDecode t);
  }


}

