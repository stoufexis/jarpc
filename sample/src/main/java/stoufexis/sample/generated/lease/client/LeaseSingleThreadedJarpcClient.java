package stoufexis.sample.generated.lease.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;

import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;

import stoufexis.jarpc.lib.util.*;
import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import static stoufexis.jarpc.lib.util.Util.*;

import static stoufexis.sample.generated.lease.common.LeaseMetadata.*;
import stoufexis.sample.generated.lease.common.*;

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

      ErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);

    this.acquireResponseHandler = acquireHandler;
    this.refreshResponseHandler = refreshHandler;
    this.queryResponseHandler = queryHandler;

  }

  public static LeaseSingleThreadedJarpcClient create(
      ConnectivityConfig cfg,

      AcquireResponseHandler acquireHandler,
      RefreshResponseHandler refreshHandler,
      QueryResponseHandler queryHandler,

      ErrorHandler errorHandler) {
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
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(ACQUIRE_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, ACQUIRE_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    acquireRequestEncode.set(claim.buffer(), newOffset);
    return acquireRequestEncode;
  }

  private boolean onAcquireResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == ACQUIRE_RESPONSE_SIZE) {
      acquireResponseDecode.set(buffer, offset);
      return acquireResponseHandler.onResponse(correlationId, acquireResponseDecode);

    } else {
      return acquireResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class AcquireRequestEncodeImpl extends EncodeUtil
      implements AcquireRequestEncode {
    @Override
    public void setKey(long key) {
      buffer.putLong(0, key);
    }

    @Override
    public void setValue(Bytes value) {
      buffer.putBytes16(8, value);
    }

  }

  private static final class AcquireResponseDecodeImpl extends DecodeUtil
      implements AcquireResponseDecode {
    @Override
    public boolean getAcquired() {
      return buffer.getBoolean(0);
    }

  }
  @Override
  public RefreshRequestEncode claimRefresh(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(REFRESH_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, REFRESH_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    refreshRequestEncode.set(claim.buffer(), newOffset);
    return refreshRequestEncode;
  }

  private boolean onRefreshResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == REFRESH_RESPONSE_SIZE) {
      refreshResponseDecode.set(buffer, offset);
      return refreshResponseHandler.onResponse(correlationId, refreshResponseDecode);

    } else {
      return refreshResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class RefreshRequestEncodeImpl extends EncodeUtil
      implements RefreshRequestEncode {
    @Override
    public void setKey(long key) {
      buffer.putLong(0, key);
    }

  }

  private static final class RefreshResponseDecodeImpl extends DecodeUtil
      implements RefreshResponseDecode {
    @Override
    public boolean getAcquired() {
      return buffer.getBoolean(0);
    }

  }
  @Override
  public QueryRequestEncode claimQuery(ClaimHandle claimHandle) {
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(QUERY_REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, QUERY_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    queryRequestEncode.set(claim.buffer(), newOffset);
    return queryRequestEncode;
  }

  private boolean onQueryResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == QUERY_RESPONSE_SIZE) {
      queryResponseDecode.set(buffer, offset);
      return queryResponseHandler.onResponse(correlationId, queryResponseDecode);

    } else {
      return queryResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class QueryRequestEncodeImpl extends EncodeUtil
      implements QueryRequestEncode {
    @Override
    public void setKey(long key) {
      buffer.putLong(0, key);
    }

  }

  private static final class QueryResponseDecodeImpl extends DecodeUtil
      implements QueryResponseDecode {
    @Override
    public boolean getExists() {
      return buffer.getBoolean(0);
    }

    @Override
    public Bytes getValue() {
      return buffer.getBytes16(1);
    }

    @Override
    public int getExpiresInSeconds() {
      return buffer.getInt(17);
    }

  }

}

