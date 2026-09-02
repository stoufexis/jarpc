package _package_.server;

import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.server.*;

import _package_.server._Service_SingleThreadedServer.*;

public interface _Service_SingleThreadedStateMachine
    extends ClientHook,
    /// foreachType
    _Type_RequestHandler,
    /// foreachType
    ServerErrorHandler {
}
