package stoufexis.sample.generated.updates.server;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;
import static stoufexis.jarpc.lib.util.Util.illegal;

import stoufexis.sample.generated.updates.common.*;
import static stoufexis.sample.generated.updates.common.UpdatesMetadata.*;

public class UpdatesSingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements UpdatesSingleThreadedServer, AutoCloseable {


  private final SubscribeOrderUpdatesRequestHandler subscribeOrderUpdatesRequestHandler;

  private final SubscribeOrderUpdatesResponseEncodeImpl subscribeOrderUpdatesResponseEncode = new SubscribeOrderUpdatesResponseEncodeImpl();

  private final SubscribeOrderUpdatesRequestDecodeImpl subscribeOrderUpdatesRequestDecode = new SubscribeOrderUpdatesRequestDecodeImpl();



  UpdatesSingleThreadedJarpcServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,

      SubscribeOrderUpdatesRequestHandler subscribeOrderUpdatesRequestHandler,

      ClientHook clientHook,
      ServerErrorHandler errorHandler) {
    super(subscription, publications, images, clientHook, errorHandler);

    this.subscribeOrderUpdatesRequestHandler = subscribeOrderUpdatesRequestHandler;


  }

  public static UpdatesSingleThreadedJarpcServer create(
      ConnectivityConfig cfg,

      SubscribeOrderUpdatesRequestHandler subscribeOrderUpdatesRequestHandler,

      ClientHook clientHook,
      ServerErrorHandler serverErrorHandler) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new UpdatesSingleThreadedJarpcServer(
        serverSubscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        images,

        subscribeOrderUpdatesRequestHandler,

        clientHook,
        serverErrorHandler);
  }

  protected boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length) {
    return switch (messageType) {

      case SUBSCRIBE_ORDER_UPDATES_MESSAGE_TYPE -> onSubscribeOrderUpdatesRequest(clientId, correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public SubscribeOrderUpdatesResponseEncode claimSubscribeOrderUpdates(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(SUBSCRIBE_ORDER_UPDATES_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, SUBSCRIBE_ORDER_UPDATES_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    subscribeOrderUpdatesResponseEncode.set(claim.buffer(), newOffset);
    return subscribeOrderUpdatesResponseEncode;
  }

  private boolean onSubscribeOrderUpdatesRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != SUBSCRIBE_ORDER_UPDATES_REQUEST_SIZE) {
      throw illegal("Unable to process SubscribeOrderUpdatesRequest");
    }

    subscribeOrderUpdatesRequestDecode.set(buffer, offset);
    return subscribeOrderUpdatesRequestHandler.onRequest(clientId, correlationId, subscribeOrderUpdatesRequestDecode);
  }




  private static final class SubscribeOrderUpdatesRequestDecodeImpl extends DecodeUtil
      implements SubscribeOrderUpdatesRequestDecode {
  }

  private static final class SubscribeOrderUpdatesResponseEncodeImpl extends EncodeUtil
      implements SubscribeOrderUpdatesResponseEncode {
    @Override
    public void setAssetId(int assetId) {
      buffer.putInt(offset + 0, assetId);
    }

    @Override
    public void setRequestedQuantityUnscaled(long requestedQuantityUnscaled) {
      buffer.putLong(offset + 4, requestedQuantityUnscaled);
    }

    @Override
    public void setFilledQuantityUnscaled(long filledQuantityUnscaled) {
      buffer.putLong(offset + 12, filledQuantityUnscaled);
    }

    @Override
    public void setRateUnscaled(long rateUnscaled) {
      buffer.putLong(offset + 20, rateUnscaled);
    }

    @Override
    public void setQuantityScale(int quantityScale) {
      buffer.putInt(offset + 28, quantityScale);
    }

    @Override
    public void setRateScale(int rateScale) {
      buffer.putInt(offset + 32, rateScale);
    }

    @Override
    public void setStatusCode(int statusCode) {
      buffer.putInt(offset + 36, statusCode);
    }

  }

}

