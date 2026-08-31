package stoufexis.jarpc.lib.client;

import stoufexis.jarpc.exchange.client.SingleThreadedJarpcExchangeClient;

public final class ConnectivityProbe {

  private final SingleThreadedJarpcExchangeClient singleThreadedClient;
  private volatile boolean isConnected = false;
  private boolean isConnectedLocal = false;

  public ConnectivityProbe(SingleThreadedJarpcExchangeClient singleThreadedClient) {
    this.singleThreadedClient = singleThreadedClient;
  }

  /**
   * Poll the single threaded client's connected status. Must only be used by the same thread that
   * uses the single threaded client
   */
  public void probeConnected() {
    // avoids a volatile read per duty cycle by using a non-volatile as reference
    if (!isConnectedLocal && singleThreadedClient.isConnected()) {
      isConnected = true;
      isConnectedLocal = true;
    }
  }

  /** Thread-safe */
  public boolean isConnected() {
    return this.isConnected;
  }
}
