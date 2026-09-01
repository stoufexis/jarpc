package stoufexis.jarpc.lib.model;

public interface ClientHook {
  void onClientDisconnected(long clientId);
}
