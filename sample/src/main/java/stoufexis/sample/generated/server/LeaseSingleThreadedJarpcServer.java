package stoufexis.sample.generated.server;

import io.aeron.Subscription;
import io.aeron.Aeron;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.common.*;

import static stoufexis.jarpc.lib.common.Util.createServerSubscription;
import static stoufexis.jarpc.lib.common.Util.illegal;

import stoufexis.sample.generated.common.*;
import static stoufexis.sample.generated.common.LeaseMetadata.*;

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
      Aeron aeron,
      Subscription subscription,
      Images images,
      LeaseSingleThreadedStateMachine stateMachine,
      ConnectionConfig connectionConfig) {
    super(aeron, subscription, images, stateMachine, connectionConfig);
    this.stateMachine = stateMachine;
  }

  public static LeaseSingleThreadedJarpcServer create(
      Aeron aeron, ConnectionConfig cfg, LeaseSingleThreadedStateMachine stateMachine) {
    Images images = new Images();
    return new LeaseSingleThreadedJarpcServer(
        aeron,
        createServerSubscription(aeron, images, cfg),
        images,
        stateMachine,
        cfg);
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

    Publisher.tryClaim(ACQUIRE_RESPONSE_SIZE, clientId, correlationId, publications, claimHandle);
    if (claimHandle.isFailed()) return null;
    acquireResponseEncode.set(claimHandle, Publisher.encodeHeader(correlationId, ACQUIRE_MESSAGE_TYPE, claimHandle));
    return acquireResponseEncode;
  }

  private boolean onAcquireRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != ACQUIRE_REQUEST_SIZE) throw illegal("Unable to process AcquireRequest");
    acquireRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, acquireRequestDecode, this);
  }

  private static final class AcquireRequestDecodeImpl extends DecodeUtil
      implements AcquireRequestDecode {
    @Override
    public long key() {
      return getLong(0);
    }

    @Override
    public Bytes value() {
      return getBytes16(8);
    }

  }

  private static final class AcquireResponseEncodeImpl extends EncodeUtil
      implements AcquireResponseEncode {
    @Override
    public void setAcquired(boolean acquired) {
      putBoolean(0, acquired);
    }

  }
  @Override
  public RefreshResponseEncode claimRefresh(
      long clientId, long correlationId, ClaimHandle claimHandle) {

    Publisher.tryClaim(REFRESH_RESPONSE_SIZE, clientId, correlationId, publications, claimHandle);
    if (claimHandle.isFailed()) return null;
    refreshResponseEncode.set(claimHandle, Publisher.encodeHeader(correlationId, REFRESH_MESSAGE_TYPE, claimHandle));
    return refreshResponseEncode;
  }

  private boolean onRefreshRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != REFRESH_REQUEST_SIZE) throw illegal("Unable to process RefreshRequest");
    refreshRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, refreshRequestDecode, this);
  }

  private static final class RefreshRequestDecodeImpl extends DecodeUtil
      implements RefreshRequestDecode {
    @Override
    public long key() {
      return getLong(0);
    }

  }

  private static final class RefreshResponseEncodeImpl extends EncodeUtil
      implements RefreshResponseEncode {
    @Override
    public void setAcquired(boolean acquired) {
      putBoolean(0, acquired);
    }

  }
  @Override
  public QueryResponseEncode claimQuery(
      long clientId, long correlationId, ClaimHandle claimHandle) {

    Publisher.tryClaim(QUERY_RESPONSE_SIZE, clientId, correlationId, publications, claimHandle);
    if (claimHandle.isFailed()) return null;
    queryResponseEncode.set(claimHandle, Publisher.encodeHeader(correlationId, QUERY_MESSAGE_TYPE, claimHandle));
    return queryResponseEncode;
  }

  private boolean onQueryRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != QUERY_REQUEST_SIZE) throw illegal("Unable to process QueryRequest");
    queryRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, queryRequestDecode, this);
  }

  private static final class QueryRequestDecodeImpl extends DecodeUtil
      implements QueryRequestDecode {
    @Override
    public long key() {
      return getLong(0);
    }

  }

  private static final class QueryResponseEncodeImpl extends EncodeUtil
      implements QueryResponseEncode {
    @Override
    public void setExists(boolean exists) {
      putBoolean(0, exists);
    }

    @Override
    public void setValue(Bytes value) {
      putBytes16(1, value);
    }

    @Override
    public void setExpiresInSeconds(int expiresInSeconds) {
      putInt(17, expiresInSeconds);
    }

  }

}

