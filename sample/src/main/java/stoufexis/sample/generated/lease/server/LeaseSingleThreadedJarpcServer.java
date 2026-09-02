package stoufexis.sample.generated.lease.server;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;
import static stoufexis.jarpc.lib.util.Util.illegal;

import stoufexis.sample.generated.lease.common.*;
import static stoufexis.sample.generated.lease.common.LeaseMetadata.*;

public class LeaseSingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements LeaseSingleThreadedServer, AutoCloseable {

  private final LeaseSingleThreadedStateMachine stateMachine;


  private final AcquireResponseEncodeImpl acquireResponseEncode = new AcquireResponseEncodeImpl();

  private final AcquireRequestDecodeImpl acquireRequestDecode = new AcquireRequestDecodeImpl();

  private final RefreshResponseEncodeImpl refreshResponseEncode = new RefreshResponseEncodeImpl();

  private final RefreshRequestDecodeImpl refreshRequestDecode = new RefreshRequestDecodeImpl();

  private final QueryResponseEncodeImpl queryResponseEncode = new QueryResponseEncodeImpl();

  private final QueryRequestDecodeImpl queryRequestDecode = new QueryRequestDecodeImpl();



  LeaseSingleThreadedJarpcServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,
      LeaseSingleThreadedStateMachine stateMachine) {
    super(subscription, publications, images, stateMachine, stateMachine);
    this.stateMachine = stateMachine;
  }

  public static LeaseSingleThreadedJarpcServer create(
      ConnectivityConfig cfg, LeaseSingleThreadedStateMachine stateMachine) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new LeaseSingleThreadedJarpcServer(
        serverSubscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        images,
        stateMachine);
  }

  protected boolean onMessage(
      long clientId,
      int messageType,
      long correlationId,
      DirectBuffer buffer,
      int offset,
      int length) {
    return switch (messageType) {

      case ACQUIRE_MESSAGE_TYPE -> onAcquireRequest(clientId, correlationId, buffer, offset, length);
      case REFRESH_MESSAGE_TYPE -> onRefreshRequest(clientId, correlationId, buffer, offset, length);
      case QUERY_MESSAGE_TYPE -> onQueryRequest(clientId, correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public AcquireResponseEncode claimAcquire(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(ACQUIRE_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, ACQUIRE_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    acquireResponseEncode.set(claim.buffer(), newOffset);
    return acquireResponseEncode;
  }

  private boolean onAcquireRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != ACQUIRE_REQUEST_SIZE) {
      throw illegal("Unable to process AcquireRequest");
    }

    acquireRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, acquireRequestDecode, this);
  }

  @Override
  public RefreshResponseEncode claimRefresh(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(REFRESH_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, REFRESH_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    refreshResponseEncode.set(claim.buffer(), newOffset);
    return refreshResponseEncode;
  }

  private boolean onRefreshRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != REFRESH_REQUEST_SIZE) {
      throw illegal("Unable to process RefreshRequest");
    }

    refreshRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, refreshRequestDecode, this);
  }

  @Override
  public QueryResponseEncode claimQuery(
      long clientId, long correlationId, ClaimHandle claimHandle) {
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(QUERY_RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, QUERY_MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    queryResponseEncode.set(claim.buffer(), newOffset);
    return queryResponseEncode;
  }

  private boolean onQueryRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != QUERY_REQUEST_SIZE) {
      throw illegal("Unable to process QueryRequest");
    }

    queryRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, queryRequestDecode, this);
  }




  private static final class AcquireRequestDecodeImpl extends DecodeUtil
      implements AcquireRequestDecode {
    @Override
    public long getKey() {
      return buffer.getLong(0);
    }

    @Override
    public Bytes getValue() {
      return buffer.getBytes16(8);
    }

  }

  private static final class AcquireResponseEncodeImpl extends EncodeUtil
      implements AcquireResponseEncode {
    @Override
    public void setAcquired(boolean acquired) {
      buffer.putBoolean(0, acquired);
    }

  }
  private static final class RefreshRequestDecodeImpl extends DecodeUtil
      implements RefreshRequestDecode {
    @Override
    public long getKey() {
      return buffer.getLong(0);
    }

  }

  private static final class RefreshResponseEncodeImpl extends EncodeUtil
      implements RefreshResponseEncode {
    @Override
    public void setAcquired(boolean acquired) {
      buffer.putBoolean(0, acquired);
    }

  }
  private static final class QueryRequestDecodeImpl extends DecodeUtil
      implements QueryRequestDecode {
    @Override
    public long getKey() {
      return buffer.getLong(0);
    }

  }

  private static final class QueryResponseEncodeImpl extends EncodeUtil
      implements QueryResponseEncode {
    @Override
    public void setExists(boolean exists) {
      buffer.putBoolean(0, exists);
    }

    @Override
    public void setValue(Bytes value) {
      buffer.putBytes16(1, value);
    }

    @Override
    public void setExpiresInSeconds(int expiresInSeconds) {
      buffer.putInt(17, expiresInSeconds);
    }

  }

}

