package stoufexis.jarpc.lib.client.concurrent;

/**
 * Utility for capturing the connected status from a single threaded service (e.g. exclusive
 * publication) and exposing it in a thread-safe manner.
 */
public final class ConnectivityProbe {

  private final IsConnected service;
  private volatile boolean isConnected = false;
  private boolean isConnectedLocal = false;

  public ConnectivityProbe(IsConnected service) {
    this.service = service;
  }

  /**
   * Poll the single threaded client's connected status. Must only be used by the same thread that
   * uses the single threaded client
   */
  public void probeConnected() {
    // avoids a volatile read per duty cycle by using a non-volatile as reference
    if (!isConnectedLocal && service.isConnected()) {
      isConnected = true;
      isConnectedLocal = true;
    }
  }

  /**
   * Thread-safe. This flag is only set once from false to true. It never reverts to false, even if
   * the underlying service starts to return false. It is therefore only useful to detect when a
   * service successfully starts up, not when it fails.
   */
  public boolean isConnected() {
    return this.isConnected;
  }
}
