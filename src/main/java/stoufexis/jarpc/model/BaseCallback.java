package stoufexis.jarpc.model;

import java.nio.ByteBuffer;

public interface BaseCallback {
  void onNotConnected(long correlationId);

  void onBackpressure(long correlationId);

  void onTimeout(long correlationId);

  void onInterrupt(long correlationId);

  void onCorruptSession(long correlationId);

  void onClientDecodeError(long correlationId, RuntimeException exception);

  void onServerDecodeError(long correlationId, ByteBuffer message, int messageSize);

  void onDuplicateId(long correlationId);
}
