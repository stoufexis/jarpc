package stoufexis.sample.generated.client;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.concurrent.*;
import stoufexis.jarpc.lib.common.*;

import stoufexis.sample.generated.common.*;

public interface LeaseConcurrentClient {

  boolean acquire(AcquireRequestDecode request, AcquireResponseHandler response);

  interface AcquireResponseHandler extends OnClientDecodeError {
    boolean onResponse(AcquireResponseDecode t);
  }
  boolean refresh(RefreshRequestDecode request, RefreshResponseHandler response);

  interface RefreshResponseHandler extends OnClientDecodeError {
    boolean onResponse(RefreshResponseDecode t);
  }
  boolean query(QueryRequestDecode request, QueryResponseHandler response);

  interface QueryResponseHandler extends OnClientDecodeError {
    boolean onResponse(QueryResponseDecode t);
  }

}

