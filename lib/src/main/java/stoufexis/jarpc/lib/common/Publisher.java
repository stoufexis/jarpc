package stoufexis.jarpc.lib.common;

import static stoufexis.jarpc.lib.common.Util.interpretErrorCode;

import io.aeron.Publication;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.lib.common.internal.MessageHeaderCodec;
import stoufexis.jarpc.lib.server.internal.ServerPublications;

/** Utility that encapsulates header encoding and error handling of publications. */
public final class Publisher {
  private final Publication publication;
  private long correlationId = 0;

  public Publisher(Publication publication) {
    this.publication = publication;
  }

  public void tryClaim(int length, ClaimHandle claimHandle) {
    long result =
        publication.tryClaim(length + MessageHeaderCodec.HEADER_SIZE, claimHandle.getClaim());

    if (result <= 0) {
      claimHandle.setFailed(interpretErrorCode(result));
    } else {
      claimHandle.setSuccess(++correlationId);
    }
  }

  public int encodeHeader(int messageType, ClaimHandle claim) {
    return encodeHeader(correlationId, messageType, claim);
  }

  public static int encodeHeader(long correlationId, int messageType, ClaimHandle claim) {
    MutableDirectBuffer buffer = claim.getClaim().buffer();
    int offset = claim.getClaim().offset();
    MessageHeaderCodec.encode(buffer, offset, correlationId, messageType);
    return offset + MessageHeaderCodec.HEADER_SIZE;
  }

  public static void tryClaim(
      int length,
      long clientId,
      long correlationId,
      ServerPublications publications,
      ClaimHandle claimHandle) {
    Publication publication = publications.get(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return;
    }

    long result =
        publication.tryClaim(length + MessageHeaderCodec.HEADER_SIZE, claimHandle.getClaim());

    if (result <= 0) {
      claimHandle.setFailed(interpretErrorCode(result));
    } else {
      claimHandle.setSuccess(correlationId);
    }
  }
}
