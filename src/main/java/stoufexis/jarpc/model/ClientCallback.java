package stoufexis.jarpc.model;

import java.nio.ByteBuffer;

public interface ClientCallback {
  void onClientDecodeError(long correlationId, RuntimeException exception);

  void onServerDecodeError(long correlationId, ByteBuffer message, int messageSize);
}
