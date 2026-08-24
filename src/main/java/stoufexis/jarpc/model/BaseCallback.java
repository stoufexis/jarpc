package stoufexis.jarpc.model;

public interface BaseCallback {

  enum ErrorType {
    NOT_CONNECTED,
    BACKPRESSURE,
    TIMEOUT,
    INTERRUPT,
    CORRUPT_SESSION
  }

  void onError(long correlationId, ErrorType reason);
}
