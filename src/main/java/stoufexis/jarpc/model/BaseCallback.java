package stoufexis.jarpc.model;

public interface BaseCallback {

  enum ErrorType {
    NOT_CONNECTED,
    BACKPRESSURE,
    TIMEOUT,
    INTERRUPT,
    CORRUPT_SESSION,
    DECODE_ERROR
  }

  /**
   * Called when a request failed to be sent or the response was not readable.
   *
   * @param correlationId id of the request
   * @param type categorization of the error
   * @param err exception that caused this error. May be {@code null}
   */
  void onError(long correlationId, ErrorType type, RuntimeException err);
}
