package stoufexis.jarpc.lib.integration.generated.client;

import io.aeron.Publication;
import io.aeron.Subscription;

import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.client.singlethread.SingleThreadedJarpcClient;
import stoufexis.jarpc.lib.common.*;

import static stoufexis.jarpc.lib.common.Util.*;

import static stoufexis.jarpc.lib.integration.generated.common.EchoMetadata.*;
import stoufexis.jarpc.lib.integration.generated.common.*;

public final class EchoSingleThreadedJarpcClient extends SingleThreadedJarpcClient
    implements EchoSingleThreadedClient {

  private final EchoRequestEncodeImpl echoRequestEncode = new EchoRequestEncodeImpl();
  private final EchoResponseDecodeImpl echoResponseDecode = new EchoResponseDecodeImpl();
  private final EchoResponseHandler echoResponseHandler;

  EchoSingleThreadedJarpcClient(
      Publication publication,
      Subscription subscription,

      EchoResponseHandler echoHandler,

      ClientErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);

    this.echoResponseHandler = echoHandler;

  }

  public static EchoSingleThreadedJarpcClient create(
      ConnectionConfig cfg,

      EchoResponseHandler echoHandler,

      ClientErrorHandler errorHandler) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new EchoSingleThreadedJarpcClient(
        pub,
        sub,

        echoHandler,

        errorHandler);
  }

  @Override
  protected boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {
    return switch (messageType) {

      case ECHO_MESSAGE_TYPE -> onEchoResponse(correlationId, buffer, offset, length);

      default -> throw illegal("Unknown message type " + messageType);
    };
  }


  @Override
  public EchoRequestEncode claimEcho(ClaimHandle claimHandle) {

    publisher.tryClaim(ECHO_REQUEST_SIZE, claimHandle);
    if (claimHandle.isFailed()) return null;
    echoRequestEncode.set(claimHandle, publisher.encodeHeader(ECHO_MESSAGE_TYPE, claimHandle));
    return echoRequestEncode;
  }

  private boolean onEchoResponse(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != ECHO_RESPONSE_SIZE) {
      return echoResponseHandler.onClientDecodeError(correlationId);
    }
    echoResponseDecode.set(buffer, offset);
    return echoResponseHandler.onResponse(correlationId, echoResponseDecode);
  }

  private static final class EchoRequestEncodeImpl extends EncodeUtil
      implements EchoRequestEncode {
    @Override
    public void setBool(boolean bool) {
      putBoolean(0, bool);
    }

    @Override
    public void setBite(byte bite) {
      putByte(1, bite);
    }

    @Override
    public void setSort(short sort) {
      putShort(2, sort);
    }

    @Override
    public void setEent(int eent) {
      putInt(4, eent);
    }

    @Override
    public void setLog(long log) {
      putLong(8, log);
    }

    @Override
    public void setFlowt(float flowt) {
      putFloat(16, flowt);
    }

    @Override
    public void setTwice(double twice) {
      putDouble(20, twice);
    }

    @Override
    public void setSBytes(Bytes sBytes) {
      putBytes16(28, sBytes);
    }

    @Override
    public void setMBytes(Bytes mBytes) {
      putBytes32(44, mBytes);
    }

    @Override
    public void setLBytes(Bytes lBytes) {
      putBytes64(76, lBytes);
    }

  }

  private static final class EchoResponseDecodeImpl extends DecodeUtil
      implements EchoResponseDecode {
    @Override
    public Bytes lBytes() {
      return getBytes64(0);
    }

    @Override
    public Bytes mBytes() {
      return getBytes32(64);
    }

    @Override
    public Bytes sBytes() {
      return getBytes16(96);
    }

    @Override
    public double twice() {
      return getDouble(112);
    }

    @Override
    public float flowt() {
      return getFloat(120);
    }

    @Override
    public long log() {
      return getLong(124);
    }

    @Override
    public int eent() {
      return getInt(132);
    }

    @Override
    public short sort() {
      return getShort(136);
    }

    @Override
    public byte bite() {
      return getByte(138);
    }

    @Override
    public boolean bool() {
      return getBoolean(139);
    }

  }

}

