package stoufexis.jarpc.model;

public interface BaseCallback {
  enum CancellationReason {
    TIMEOUT,
    INTERRUPT
  }

  void onCancelled(long correlationId, CancellationReason reason);
}
