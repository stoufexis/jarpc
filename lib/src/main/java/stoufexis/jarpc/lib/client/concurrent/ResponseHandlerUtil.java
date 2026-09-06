package stoufexis.jarpc.lib.client.concurrent;

import org.agrona.collections.Long2ObjectHashMap;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.client.OnClientDecodeError;

/**
 * Base class for response handler implementations. Exposes the components that are required to
 * implement each handler, while implementing generically as much of the handler as possible,
 * without introducing extreme abstraction.
 */
public abstract class ResponseHandlerUtil<T extends OnClientDecodeError>
    implements OnClientDecodeError {
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
    if (dispatched) removeCallback(correlationId);

    return dispatched;
  }
}
