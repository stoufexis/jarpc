package stoufexis.jarpc.client;

import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.model.MessageHeaderCodec;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public final class Publisher {
  private final Publication publication;
  private long correlationId = 0;

  public Publisher(Publication publication) {
    this.publication = publication;
  }

  public long nextCorrelationId() {
    return correlationId++;
  }

  public ErrorCode tryClaim(int length, BufferClaim claim) {
    long result = publication.tryClaim(length + MessageHeaderCodec.HEADER_SIZE, claim);
    return result <= 0 ? interpretErrorCode(result) : null;
  }

  public int encodeHeader(long correlationId, int messageType, BufferClaim claim) {
    MutableDirectBuffer buffer = claim.buffer();
    int offset = claim.offset();
    MessageHeaderCodec.encode(buffer, offset, correlationId, messageType);
    return offset + MessageHeaderCodec.HEADER_SIZE;
  }
}
