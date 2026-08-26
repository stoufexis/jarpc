package stoufexis.jarpc.exchange.server;

import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.server.JarpcServer;
import stoufexis.jarpc.server.ServerErrorHandler;

import static stoufexis.jarpc.util.Util.interpretErrorCode;

public final class JarpcExchangeServer extends JarpcServer {
  private final ExchangeServer exchange;

  private final PostOrderRequest postOrderRequest = new PostOrderRequest();
  private final CancelAllRequest cancelAllRequest = new CancelAllRequest();
  private final PostOrderCallbackImpl postOrderCallback = new PostOrderCallbackImpl();
  private final CancelAllCallbackImpl cancelAllCallback = new CancelAllCallbackImpl();

  public JarpcExchangeServer(ServerErrorHandler errorHandler, ExchangeServer exchange) {
    super(errorHandler);
    this.exchange = exchange;
  }

  @Override
  protected boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length) {
    switch (messageType) {
      case Catalog.postOrderId -> {
        try {
          postOrderRequest.decode(buffer, offset, length);
          return exchange.postOrder(clientId, correlationId, postOrderRequest, postOrderCallback);

        } catch (RuntimeException e) {
          return sendDecodeFailure(clientId, correlationId, messageType);
        }
      }

      case Catalog.cancelAllOrdersId -> {
        try {
          cancelAllRequest.decode(buffer, offset, length);
          return exchange.cancelAll(clientId, correlationId, cancelAllRequest, cancelAllCallback);

        } catch (RuntimeException e) {
          return sendDecodeFailure(clientId, correlationId, messageType);
        }
      }

      default -> {
        return sendDecodeFailure(clientId, correlationId, messageType);
      }
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
