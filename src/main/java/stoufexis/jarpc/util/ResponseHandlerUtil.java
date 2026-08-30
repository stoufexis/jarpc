package stoufexis.jarpc.util;

import org.agrona.collections.Long2ObjectHashMap;
import stoufexis.jarpc.client.ClientErrorHandler;
import stoufexis.jarpc.client.ClientHandler;

public abstract class ResponseHandlerUtil<T extends ClientHandler> implements ClientHandler {
  private final Long2ObjectHashMap<T> callbacks;
  private final ClientErrorHandler errorHandler;
  private final String label;

  protected ResponseHandlerUtil(
      Long2ObjectHashMap<T> callbacks, ClientErrorHandler errorHandler, String label) {
    this.callbacks = callbacks;
    this.errorHandler = errorHandler;
    this.label = label;
  }

  public final T getCallback(long correlationId) {
    T callback = callbacks.get(correlationId);

    if (callback == null) {
      errorHandler.onCallbackNotFound(correlationId, label);
    }

    return callback;
  }

  public final void removeCallback(long correlationId) {
    callbacks.remove(correlationId);
  }

  @Override
  public final boolean onClientDecodeError(long correlationId) {
    T callback = getCallback(correlationId);

    if (callback == null) return true;

    boolean dispatched = callback.onClientDecodeError(correlationId);

    if (dispatched) callbacks.remove(correlationId);

    return dispatched;
  }
}
