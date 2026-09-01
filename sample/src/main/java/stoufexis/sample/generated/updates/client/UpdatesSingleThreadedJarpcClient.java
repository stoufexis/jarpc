package stoufexis.sample.generated.updates.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;

import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;

import stoufexis.jarpc.lib.util.*;
import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import static stoufexis.jarpc.lib.util.Util.*;

import static stoufexis.sample.generated.updates.common.UpdatesMetadata.*;
import stoufexis.sample.generated.updates.common.*;

public final class UpdatesSingleThreadedJarpcClient extends SingleThreadedJarpcClient
    implements UpdatesSingleThreadedClient {


  private final SubscribeOrderUpdatesRequestEncodeImpl subscribeOrderUpdatesRequestEncode = new SubscribeOrderUpdatesRequestEncodeImpl();

  private final SubscribeOrderUpdatesResponseDecodeImpl subscribeOrderUpdatesResponseDecode = new SubscribeOrderUpdatesResponseDecodeImpl();

  private final SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesResponseHandler;



  UpdatesSingleThreadedJarpcClient(
      Publication publication,
      Subscription subscription,

      SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesHandler,

      ErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);

    this.subscribeOrderUpdatesResponseHandler = subscribeOrderUpdatesHandler;

  }

  public static UpdatesSingleThreadedJarpcClient create(
      ConnectivityConfig cfg,

      SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesHandler,

      ErrorHandler errorHandler) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new UpdatesSingleThreadedJarpcClient(
        pub,
        sub,

        subscribeOrderUpdatesHandler,

        errorHandler);
  }

  @Override
  protected boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {
    return switch (messageType) {

      case SUBSCRIBE_ORDER_UPDATES_MESSAGE_TYPE -> onSubscribeOrderUpdatesResponse(correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public SubscribeOrderUpdatesRequestEncode claimSubscribeOrderUpdates(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(SUBSCRIBE_ORDER_UPDATES_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, SUBSCRIBE_ORDER_UPDATES_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    subscribeOrderUpdatesRequestEncode.set(claim.buffer(), newOffset);
    return subscribeOrderUpdatesRequestEncode;
  }

  private boolean onSubscribeOrderUpdatesResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == SUBSCRIBE_ORDER_UPDATES_RESPONSE_SIZE) {
      subscribeOrderUpdatesResponseDecode.set(buffer, offset);
      return subscribeOrderUpdatesResponseHandler.onResponse(correlationId, subscribeOrderUpdatesResponseDecode);

    } else {
      return subscribeOrderUpdatesResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class SubscribeOrderUpdatesRequestEncodeImpl extends EncodeUtil
      implements SubscribeOrderUpdatesRequestEncode {
  }

  private static final class SubscribeOrderUpdatesResponseDecodeImpl extends DecodeUtil
      implements SubscribeOrderUpdatesResponseDecode {
    @Override
    public int getAssetId() {
      return buffer.getInt(offset + 0);
    }

    @Override
    public long getRequestedQuantityUnscaled() {
      return buffer.getLong(offset + 4);
    }

    @Override
    public long getFilledQuantityUnscaled() {
      return buffer.getLong(offset + 12);
    }

    @Override
    public long getRateUnscaled() {
      return buffer.getLong(offset + 20);
    }

    @Override
    public int getQuantityScale() {
      return buffer.getInt(offset + 28);
    }

    @Override
    public int getRateScale() {
      return buffer.getInt(offset + 32);
    }

    @Override
    public int getStatusCode() {
      return buffer.getInt(offset + 36);
    }

  }

}

