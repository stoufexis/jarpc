package stoufexis.sample.generated.client;

import io.aeron.Publication;
import io.aeron.Subscription;

import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.singlethread.SingleThreadedJarpcClient;
import stoufexis.jarpc.lib.common.*;

import static stoufexis.jarpc.lib.common.Util.*;

import static stoufexis.sample.generated.common.LeaseMetadata.*;
import stoufexis.sample.generated.common.*;

public final class LeaseSingleThreadedJarpcClient extends SingleThreadedJarpcClient
    implements LeaseSingleThreadedClient {

  private final AcquireRequestEncodeImpl acquireRequestEncode = new AcquireRequestEncodeImpl();
  private final AcquireResponseDecodeImpl acquireResponseDecode = new AcquireResponseDecodeImpl();
  private final AcquireResponseHandler acquireResponseHandler;
  private final RefreshRequestEncodeImpl refreshRequestEncode = new RefreshRequestEncodeImpl();
  private final RefreshResponseDecodeImpl refreshResponseDecode = new RefreshResponseDecodeImpl();
  private final RefreshResponseHandler refreshResponseHandler;
  private final QueryRequestEncodeImpl queryRequestEncode = new QueryRequestEncodeImpl();
  private final QueryResponseDecodeImpl queryResponseDecode = new QueryResponseDecodeImpl();
  private final QueryResponseHandler queryResponseHandler;

  LeaseSingleThreadedJarpcClient(
      Publication publication,
      Subscription subscription,

      AcquireResponseHandler acquireHandler,
      RefreshResponseHandler refreshHandler,
      QueryResponseHandler queryHandler,

      ClientErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);

    this.acquireResponseHandler = acquireHandler;
    this.refreshResponseHandler = refreshHandler;
    this.queryResponseHandler = queryHandler;

  }

  public static LeaseSingleThreadedJarpcClient create(
      ConnectionConfig cfg,

      AcquireResponseHandler acquireHandler,
      RefreshResponseHandler refreshHandler,
      QueryResponseHandler queryHandler,

      ClientErrorHandler errorHandler) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new LeaseSingleThreadedJarpcClient(
        pub,
        sub,

        acquireHandler,
        refreshHandler,
        queryHandler,

        errorHandler);
  }

  @Override
  protected boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {
    return switch (messageType) {

      case ACQUIRE_MESSAGE_TYPE -> onAcquireResponse(correlationId, buffer, offset, length);
      case REFRESH_MESSAGE_TYPE -> onRefreshResponse(correlationId, buffer, offset, length);
      case QUERY_MESSAGE_TYPE -> onQueryResponse(correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public AcquireRequestEncode claimAcquire(ClaimHandle claimHandle) {

    publisher.tryClaim(ACQUIRE_REQUEST_SIZE, claimHandle);
    if (claimHandle.isFailed()) return null;
    acquireRequestEncode.set(claimHandle, publisher.encodeHeader(ACQUIRE_MESSAGE_TYPE, claimHandle));
    return acquireRequestEncode;
  }

  private boolean onAcquireResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != ACQUIRE_RESPONSE_SIZE) {
      return acquireResponseHandler.onClientDecodeError(correlationId);
    }
    acquireResponseDecode.set(buffer, offset);
    return acquireResponseHandler.onResponse(correlationId, acquireResponseDecode);
  }

  private static final class AcquireRequestEncodeImpl extends EncodeUtil
      implements AcquireRequestEncode {
    @Override
    public void setKey(long key) {
      putLong(0, key);
    }

    @Override
    public void setValue(Bytes value) {
      putBytes16(8, value);
    }

  }

  private static final class AcquireResponseDecodeImpl extends DecodeUtil
      implements AcquireResponseDecode {
    @Override
    public boolean acquired() {
      return getBoolean(0);
    }

  }
  @Override
  public RefreshRequestEncode claimRefresh(ClaimHandle claimHandle) {

    publisher.tryClaim(REFRESH_REQUEST_SIZE, claimHandle);
    if (claimHandle.isFailed()) return null;
    refreshRequestEncode.set(claimHandle, publisher.encodeHeader(REFRESH_MESSAGE_TYPE, claimHandle));
    return refreshRequestEncode;
  }

  private boolean onRefreshResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != REFRESH_RESPONSE_SIZE) {
      return refreshResponseHandler.onClientDecodeError(correlationId);
    }
    refreshResponseDecode.set(buffer, offset);
    return refreshResponseHandler.onResponse(correlationId, refreshResponseDecode);
  }

  private static final class RefreshRequestEncodeImpl extends EncodeUtil
      implements RefreshRequestEncode {
    @Override
    public void setKey(long key) {
      putLong(0, key);
    }

  }

  private static final class RefreshResponseDecodeImpl extends DecodeUtil
      implements RefreshResponseDecode {
    @Override
    public boolean acquired() {
      return getBoolean(0);
    }

  }
  @Override
  public QueryRequestEncode claimQuery(ClaimHandle claimHandle) {

    publisher.tryClaim(QUERY_REQUEST_SIZE, claimHandle);
    if (claimHandle.isFailed()) return null;
    queryRequestEncode.set(claimHandle, publisher.encodeHeader(QUERY_MESSAGE_TYPE, claimHandle));
    return queryRequestEncode;
  }

  private boolean onQueryResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != QUERY_RESPONSE_SIZE) {
      return queryResponseHandler.onClientDecodeError(correlationId);
    }
    queryResponseDecode.set(buffer, offset);
    return queryResponseHandler.onResponse(correlationId, queryResponseDecode);
  }

  private static final class QueryRequestEncodeImpl extends EncodeUtil
      implements QueryRequestEncode {
    @Override
    public void setKey(long key) {
      putLong(0, key);
    }

  }

  private static final class QueryResponseDecodeImpl extends DecodeUtil
      implements QueryResponseDecode {
    @Override
    public boolean exists() {
      return getBoolean(0);
    }

    @Override
    public Bytes value() {
      return getBytes16(1);
    }

    @Override
    public int expiresInSeconds() {
      return getInt(17);
    }

  }

}

