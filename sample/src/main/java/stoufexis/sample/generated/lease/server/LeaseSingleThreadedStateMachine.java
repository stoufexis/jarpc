package stoufexis.sample.generated.lease.server;

import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.server.*;

import stoufexis.sample.generated.lease.server.LeaseSingleThreadedServer.*;

public interface LeaseSingleThreadedStateMachine
    extends ClientHook,

    AcquireRequestHandler,
    RefreshRequestHandler,
    QueryRequestHandler,

    ServerErrorHandler {
}

