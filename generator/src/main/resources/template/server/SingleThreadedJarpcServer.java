package _package_.server;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.common.*;

import static stoufexis.jarpc.lib.common.Util.createServerSubscription;
import static stoufexis.jarpc.lib.common.Util.illegal;

import _package_.common.*;
import static _package_.common._Service_Metadata.*;

public class _Service_SingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements _Service_SingleThreadedServer, AutoCloseable {

  private final _Service_SingleThreadedStateMachine stateMachine;

  /// foreachType
  private final _Type_ResponseEncodeImpl _type_ResponseEncode = new _Type_ResponseEncodeImpl();
  private final _Type_RequestDecodeImpl _type_RequestDecode = new _Type_RequestDecodeImpl();
  /// foreachType

  _Service_SingleThreadedJarpcServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,
      _Service_SingleThreadedStateMachine stateMachine) {
    super(subscription, publications, images, stateMachine);
    this.stateMachine = stateMachine;
  }

  public static _Service_SingleThreadedJarpcServer create(
      ConnectionConfig cfg, _Service_SingleThreadedStateMachine stateMachine) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new _Service_SingleThreadedJarpcServer(
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
      /// foreachType
      case _TYPE__MESSAGE_TYPE -> on_Type_Request(clientId, correlationId, buffer, offset, length);
      /// foreachType
      default -> throw illegal("Unknown message type " + messageType);
    };
  }

  /// foreachType
  @Override
  public _Type_ResponseEncode claim_Type_(
      long clientId, long correlationId, ClaimHandle claimHandle) {

    Publisher.tryClaim(_TYPE__RESPONSE_SIZE, clientId, correlationId, publications, claimHandle);
    if (claimHandle.isFailed()) return null;
    _type_ResponseEncode.set(claimHandle, Publisher.encodeHeader(correlationId, _TYPE__MESSAGE_TYPE, claimHandle));
    return _type_ResponseEncode;
  }

  private boolean on_Type_Request(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != _TYPE__REQUEST_SIZE) throw illegal("Unable to process _Type_Request");
    _type_RequestDecode.set(buffer, offset);
    return stateMachine.onRequest(clientId, correlationId, _type_RequestDecode, this);
  }

  private static final class _Type_RequestDecodeImpl extends DecodeUtil
      implements _Type_RequestDecode {
    /// foreachRequestField
    @Override
    public _javaType_ _field_() {
      return get_FieldType_(_fieldOffset_);
    }
    /// foreachRequestField
  }

  private static final class _Type_ResponseEncodeImpl extends EncodeUtil
      implements _Type_ResponseEncode {
    /// foreachResponseField
    @Override
    public void set_Field_(_javaType_ _field_) {
      put_FieldType_(_fieldOffset_, _field_);
    }
    /// foreachResponseField
  }
  /// foreachType
}
