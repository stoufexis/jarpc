package _package_.server;

import stoufexis.jarpc.lib.common.*;
import stoufexis.jarpc.lib.server.*;

import _package_.server._Service_SingleThreadedServer.*;

public interface _Service_SingleThreadedStateMachine
    extends
    /// foreachType
    _Type_RequestHandler,
    /// foreachType
    ServerStateMachine
    {
}
