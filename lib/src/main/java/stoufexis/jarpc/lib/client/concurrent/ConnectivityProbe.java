package stoufexis.jarpc.lib.client.concurrent;

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

  /** Thread-safe */
  public boolean isConnected() {
    return this.isConnected;
  }
}
