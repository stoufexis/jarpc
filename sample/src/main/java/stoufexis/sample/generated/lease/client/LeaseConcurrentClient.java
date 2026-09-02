package stoufexis.sample.generated.lease.client;

import stoufexis.jarpc.lib.client.*;

import stoufexis.sample.generated.lease.common.*;

public interface LeaseConcurrentClient {

  boolean acquire(AcquireRequestDecode request, AcquireResponseHandler response);

  interface AcquireResponseHandler extends ClientHandler {
    boolean onResponse(AcquireResponseDecode t);
  }

  boolean refresh(RefreshRequestDecode request, RefreshResponseHandler response);

  interface RefreshResponseHandler extends ClientHandler {
    boolean onResponse(RefreshResponseDecode t);
  }

  boolean query(QueryRequestDecode request, QueryResponseHandler response);

  interface QueryResponseHandler extends ClientHandler {
    boolean onResponse(QueryResponseDecode t);
  }


}

