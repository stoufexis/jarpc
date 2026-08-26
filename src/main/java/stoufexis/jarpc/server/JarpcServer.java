package stoufexis.jarpc.server;

import io.aeron.*;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import stoufexis.jarpc.model.DecodeFailureResponse;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.model.MessageHeader;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public abstract class JarpcServer implements Agent, AutoCloseable {
  private static final int FRAGMENT_LIMIT = 10;

  private final MessageHeader header = new MessageHeader();
  private final DecodeFailureResponse decodeFailureResponse = new DecodeFailureResponse();
  protected final ServerPublications publications = new ServerPublications();
  private final ControlledFragmentAssembler assembled =
      new ControlledFragmentAssembler(this::onFragment);

  protected final ServerErrorHandler errorHandler;
  private final Images images;
  private final int responseStreamId;
  private final ChannelUriStringBuilder responseUriBuilder;
  private final Subscription serverSubscription;
  private final Aeron aeron;

  protected JarpcServer(
      ServerErrorHandler errorHandler,
      Images images,
      int responseStreamId,
      ChannelUriStringBuilder responseUriBuilder,
      Subscription serverSubscription,
      Aeron aeron) {
    this.errorHandler = errorHandler;
    this.images = images;
    this.responseStreamId = responseStreamId;
    this.responseUriBuilder = responseUriBuilder;
    this.serverSubscription = serverSubscription;
    this.aeron = aeron;
  }

  @Override
  public int doWork() {
    int work = 0;

    Image image;
    while (null != (image = images.pollAvailable())) {
      work++;
      ensurePublicationExists(image);
    }

    while (null != (image = images.pollUnavailable())) {
      work++;
      assembled.freeSessionBuffer(image.sessionId());
      CloseHelper.quietClose(publications.remove(image.correlationId()));
    }

    return work + serverSubscription.controlledPoll(assembled, FRAGMENT_LIMIT);
  }

  @Override
  public void close() {
    CloseHelper.quietClose(serverSubscription);
    publications.closeAll();
  }

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header aeronHeader) {
    Image image = (Image) aeronHeader.context();
    ensurePublicationExists(image);

    try {
      header.decode(buffer, offset, length);

      boolean result =
          onMessage(
              image.correlationId(),
              header.getMessageType(),
              header.getCorrelationId(),
              buffer,
              offset + MessageHeader.HEADER_SIZE,
              length - MessageHeader.HEADER_SIZE);

      return result
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      throw e;
    }
  }

  private void ensurePublicationExists(Image image) {
    // FIXME We dont need computeIfAbsent, put/remove only happen in the agent thread.
    if (null == publications.get(image.correlationId())) {
      Publication publication =
          aeron.addPublication(
              responseUriBuilder.responseCorrelationId(image.correlationId()).build(),
              responseStreamId);

      publications.put(image.correlationId(), publication);
    }
  }

  @Override
  public String roleName() {
    return "JarpcServerReceiver";
  }

  protected boolean sendDecodeFailure(long clientId, long correlationId, int baseMessageType) {
    Publication publication = publications.get(clientId);
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

  protected abstract boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length);
}
