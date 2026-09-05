package stoufexis.jarpc.lib.integration.generated.server;

import io.aeron.Subscription;
import io.aeron.Aeron;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.common.*;

import static stoufexis.jarpc.lib.common.Util.createServerSubscription;
import static stoufexis.jarpc.lib.common.Util.illegal;

import stoufexis.jarpc.lib.integration.generated.common.*;
import static stoufexis.jarpc.lib.integration.generated.common.EchoMetadata.*;

public class EchoSingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements EchoSingleThreadedServer, AutoCloseable {

  private final EchoSingleThreadedStateMachine stateMachine;


  private final EchoResponseEncodeImpl echoResponseEncode = new EchoResponseEncodeImpl();
  private final EchoRequestDecodeImpl echoRequestDecode = new EchoRequestDecodeImpl();


  EchoSingleThreadedJarpcServer(
      Aeron aeron,
      Subscription subscription,
      Images images,
      EchoSingleThreadedStateMachine stateMachine,
      ConnectionConfig connectionConfig) {
    super(aeron, subscription, images, stateMachine, connectionConfig);
    this.stateMachine = stateMachine;
  }

  public static EchoSingleThreadedJarpcServer create(
      Aeron aeron, ConnectionConfig cfg, EchoSingleThreadedStateMachine stateMachine) {
    Images images = new Images();
    return new EchoSingleThreadedJarpcServer(
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

      case ECHO_MESSAGE_TYPE -> onEchoRequest(clientId, correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public EchoResponseEncode claimEcho(
      long clientId, long correlationId, ClaimHandle claimHandle) {

    Publisher.tryClaim(ECHO_RESPONSE_SIZE, clientId, correlationId, publications, claimHandle);
    if (claimHandle.isFailed()) return null;
    echoResponseEncode.set(claimHandle, Publisher.encodeHeader(correlationId, ECHO_MESSAGE_TYPE, claimHandle));
    return echoResponseEncode;
  }

  private boolean onEchoRequest(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != ECHO_REQUEST_SIZE) throw illegal("Unable to process EchoRequest");
    echoRequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, echoRequestDecode, this);
  }

  private static final class EchoRequestDecodeImpl extends DecodeUtil
      implements EchoRequestDecode {
    @Override
    public boolean bool() {
      return getBoolean(0);
    }

    @Override
    public byte bite() {
      return getByte(1);
    }

    @Override
    public short sort() {
      return getShort(2);
    }

    @Override
    public int eent() {
      return getInt(4);
    }

    @Override
    public long log() {
      return getLong(8);
    }

    @Override
    public float flowt() {
      return getFloat(16);
    }

    @Override
    public double twice() {
      return getDouble(20);
    }

    @Override
    public Bytes sBytes() {
      return getBytes16(28);
    }

    @Override
    public Bytes mBytes() {
      return getBytes32(44);
    }

    @Override
    public Bytes lBytes() {
      return getBytes64(76);
    }

  }

  private static final class EchoResponseEncodeImpl extends EncodeUtil
      implements EchoResponseEncode {
    @Override
    public void setLBytes(Bytes lBytes) {
      putBytes64(0, lBytes);
    }

    @Override
    public void setMBytes(Bytes mBytes) {
      putBytes32(64, mBytes);
    }

    @Override
    public void setSBytes(Bytes sBytes) {
      putBytes16(96, sBytes);
    }

    @Override
    public void setTwice(double twice) {
      putDouble(112, twice);
    }

    @Override
    public void setFlowt(float flowt) {
      putFloat(120, flowt);
    }

    @Override
    public void setLog(long log) {
      putLong(124, log);
    }

    @Override
    public void setEent(int eent) {
      putInt(132, eent);
    }

    @Override
    public void setSort(short sort) {
      putShort(136, sort);
    }

    @Override
    public void setBite(byte bite) {
      putByte(138, bite);
    }

    @Override
    public void setBool(boolean bool) {
      putBoolean(139, bool);
    }

  }

}

