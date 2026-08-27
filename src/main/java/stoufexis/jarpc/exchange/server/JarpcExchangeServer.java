package stoufexis.jarpc.exchange.server;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.server.*;

import static stoufexis.jarpc.util.Util.*;

public final class JarpcExchangeServer extends JarpcServer {
  private final ExchangeServer exchange;

  private final PostOrderRequest postOrderRequest = new PostOrderRequest();
  private final CancelAllRequest cancelAllRequest = new CancelAllRequest();
  private final PostOrderCallbackImpl postOrderCallback = new PostOrderCallbackImpl();
  private final CancelAllCallbackImpl cancelAllCallback = new CancelAllCallbackImpl();

  JarpcExchangeServer(
      Aeron aeron,
      ExchangeServer exchange,
      ServerErrorHandler errorHandler,
      Images images,
      int responseStreamId,
      String responseControl,
      Subscription serverSubscription) {
    super(aeron, errorHandler, images, responseStreamId, responseControl, serverSubscription);
    this.exchange = exchange;
  }

  public static JarpcExchangeServer create(
      Aeron aeron,
      ExchangeServer exchange,
      ServerErrorHandler serverErrorHandler,
      String requestEndpoint,
      int requestStreamId,
      String responseControl,
      int responseStreamId) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(aeron, images, requestEndpoint, requestStreamId);

    return new JarpcExchangeServer(
        aeron,
        exchange,
        serverErrorHandler,
        images,
        responseStreamId,
        responseControl,
        serverSubscription);
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
        postOrderRequest.decode(buffer, offset, length);
        return exchange.postOrder(clientId, correlationId, postOrderRequest, postOrderCallback);
      }

      case Catalog.cancelAllOrdersId -> {
        cancelAllRequest.decode(buffer, offset, length);
        return exchange.cancelAll(clientId, correlationId, cancelAllRequest, cancelAllCallback);
      }

      default -> throw illegal("Unknown message type " + messageType);
    }
  }

  // All callback implementations are basically identical.
  // However, we do not introduce a generic implementation, as it would easily result in megamorphic
  // dispatch when supporting many response types.

  private class PostOrderCallbackImpl extends ServerCallback
      implements ExchangeServer.PostOrderCallback {

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

  private class CancelAllCallbackImpl extends ServerCallback
      implements ExchangeServer.CancelAllCallback {

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
