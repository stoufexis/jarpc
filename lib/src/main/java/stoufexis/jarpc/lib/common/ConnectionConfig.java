package stoufexis.jarpc.lib.common;

public record ConnectionConfig(
    String requestEndpoint,
    int requestStreamId,
    String responseControl,
    int responseStreamId,
    Media media) {

  public String mediaString() {
    return switch (media) {
      case UDP -> "udp";
      case IPC -> "ipc";
    };
  }

  public enum Media {
    UDP,
    IPC
  }
}
