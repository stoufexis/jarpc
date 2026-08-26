package stoufexis.jarpc.exchange;

import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.model.DecodeFailureResponse;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.MessageHeader;
import stoufexis.jarpc.util.ResponseServer;
import stoufexis.jarpc.util.ServerErrorHandler;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public class JarpcExchangeServer extends ResponseServer.ResponseHandler {
  private final long clientId;
  private final ExchangeServer exchange;

  private final MessageHeader header = new MessageHeader();
  private final PostOrderRequest postOrderRequest = new PostOrderRequest();
  private final CancelAllRequest cancelAllRequest = new CancelAllRequest();
  private final DecodeFailureResponse decodeFailureResponse = new DecodeFailureResponse();
  private final PostOrderCallbackImpl postOrderCallback = new PostOrderCallbackImpl();
  private final CancelAllCallbackImpl cancelAllCallback = new CancelAllCallbackImpl();

  public JarpcExchangeServer(
      ServerErrorHandler errorHandler, ExchangeServer exchange, Image image) {
    super(errorHandler);
    this.exchange = exchange;
    this.clientId = image.correlationId();
  }

  @Override
  public boolean onMessage(long clientId, DirectBuffer buffer, int offset, int length, Header h_) {
    try {
      header.decode(buffer, offset, length);

      offset += MessageHeader.HEADER_SIZE;
      length -= MessageHeader.HEADER_SIZE;

      int messageType = header.getMessageType();
      long correlationId = header.getCorrelationId();

      switch (messageType) {
        case Catalog.postOrderId -> {
          try {
            postOrderRequest.decode(buffer, offset, length);
            return exchange.postOrder(clientId, correlationId, postOrderRequest, postOrderCallback);

          } catch (RuntimeException e) {
            sendDecodeFailure(clientId, correlationId, messageType);
            return true;
          }
        }

        case Catalog.cancelAllOrdersId -> {
          try {
            cancelAllRequest.decode(buffer, offset, length);
            return exchange.cancelAll(clientId, correlationId, cancelAllRequest, cancelAllCallback);

          } catch (RuntimeException e) {
            sendDecodeFailure(clientId, correlationId, messageType);
            return true;
          }
        }

        default -> {
          sendDecodeFailure(clientId, correlationId, messageType);
          return true;
        }
      }

    } catch (RuntimeException e) {
      errorHandler.onError(e);
      throw e;
    }
  }

  private void sendDecodeFailure(long clientId, long correlationId, int baseMessageType) {
    Publication publication = getPublication(clientId);
    if (publication == null) {
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.CLIENT_NOT_EXISTS);
      return;
    }

    decodeFailureResponse.set(baseMessageType);

    BufferClaim claim = decodeFailureResponse.getClaim();
    long result = publication.tryClaim(decodeFailureResponse.getMessageSize(), claim);

    if (result < 0) {
      // Sending a decode failure is best-effort. We rely on client's timeouts in this case to
      // clean up the hanged callback.
      errorHandler.onInternalError(clientId, correlationId, interpretErrorCode(result));
    }

    try {
      decodeFailureResponse.encode(claim.buffer(), claim.offset());
      claim.commit();
    } catch (RuntimeException e) {
      claim.abort();
      errorHandler.onInternalError(clientId, correlationId, ErrorCode.ENCODE_ERROR);
    }
  }

  // All callback implementations are basically identical.
  // However, we do not introduce a generic implementation, as it would easily result in megamorphic
  // dispatch when supporting many response types.

  private class PostOrderCallbackImpl implements ExchangeServer.PostOrderCallback {

    @Override
    public ErrorCode onResponse(long clientId, long correlationId, PostOrderResponse t) {
      Publication publication = getPublication(clientId);
      if (publication == null) return ErrorCode.CLIENT_NOT_EXISTS;

      BufferClaim claim = t.getClaim();
      long result = publication.tryClaim(t.getMessageSize(), claim);

      if (result < 0) {
        return interpretErrorCode(result);
      }

      try {
        t.encode(claim.buffer(), claim.offset());
        claim.commit();
        return null;
      } catch (RuntimeException e) {
        claim.abort();
        return ErrorCode.ENCODE_ERROR;
      }
    }
  }

  private class CancelAllCallbackImpl implements ExchangeServer.CancelAllCallback {
    @Override
    public ErrorCode onResponse(long clientId, long correlationId, CancelAllResponse t) {
      Publication publication = getPublication(clientId);
      if (publication == null) return ErrorCode.CLIENT_NOT_EXISTS;

      BufferClaim claim = t.getClaim();
      long result = publication.tryClaim(t.getMessageSize(), claim);

      if (result < 0) {
        return interpretErrorCode(result);
      }

      try {
        t.encode(claim.buffer(), claim.offset());
        claim.commit();
        return null;
      } catch (RuntimeException e) {
        claim.abort();
        return ErrorCode.ENCODE_ERROR;
      }
    }
  }
}
