package stoufexis.jarpc.util;

import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.jctools.maps.NonBlockingHashMapLong;
import stoufexis.jarpc.model.DecodeFailureResponse;
import stoufexis.jarpc.model.ErrorCode;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public abstract class JarpcServer {

  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.
  private final NonBlockingHashMapLong<Publication> clientToPublicationMap =
      new NonBlockingHashMapLong<>();

  private final MessageHeader header = new MessageHeader();
  private final DecodeFailureResponse decodeFailureResponse = new DecodeFailureResponse();

  protected final ServerErrorHandler errorHandler;

  protected JarpcServer(ServerErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  protected final Publication getPublication(long clientId) {
    return clientToPublicationMap.get(clientId);
  }

  protected boolean sendDecodeFailure(long clientId, long correlationId, int baseMessageType) {
    Publication publication = getPublication(clientId);
    if (publication == null) {
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.CLIENT_NOT_EXISTS);
      return true;
    }

    decodeFailureResponse.set(baseMessageType);

    BufferClaim claim = decodeFailureResponse.getClaim();
    long result = publication.tryClaim(decodeFailureResponse.getMessageSize(), claim);

    // Sending a decode failure is best-effort.
    if (result < 0) {
      ErrorCode code = interpretErrorCode(result);

      if (code == ErrorCode.BACKPRESSURE) {
        return false;
      } else {
        errorHandler.onInternalError(clientId, correlationId, code);
        return true;
      }
    }

    try {
      decodeFailureResponse.encode(claim.buffer(), claim.offset());
      claim.commit();
      return true;
    } catch (RuntimeException e) {
      claim.abort();
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.ENCODE_ERROR);
      return true;
    }
  }

  final void putPublication(long clientId, Publication pub) {
    clientToPublicationMap.put(clientId, pub);
  }

  final Publication removePublication(long clientId) {
    return clientToPublicationMap.remove(clientId);
  }

  final void closePublications() {
    clientToPublicationMap.values().forEach(CloseHelper::quietClose);
  }

  final boolean onMessage(long clientId, DirectBuffer buffer, int offset, int length, Header h_) {
    try {
      header.decode(buffer, offset, length);

      return onMessage(
          clientId,
          header.getMessageType(),
          header.getCorrelationId(),
          buffer,
          offset + MessageHeader.HEADER_SIZE,
          length - MessageHeader.HEADER_SIZE);
    } catch (RuntimeException e) {
      errorHandler.onError(e);
      throw e;
    }
  }

  protected abstract boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length);
}
