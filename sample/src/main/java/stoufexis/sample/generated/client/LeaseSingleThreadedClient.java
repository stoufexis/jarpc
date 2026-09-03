package stoufexis.sample.generated.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.sample.generated.common.*;

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

