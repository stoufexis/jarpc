package stoufexis.sample.generated.server;

import stoufexis.jarpc.lib.common.*;
import stoufexis.jarpc.lib.server.*;

import stoufexis.sample.generated.server.LeaseSingleThreadedServer.*;

public interface LeaseSingleThreadedStateMachine
    extends

    AcquireRequestHandler,
    RefreshRequestHandler,
    QueryRequestHandler,

    ServerStateMachine
    {
}

