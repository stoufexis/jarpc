package _package_.server;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.server.*;
import stoufexis.jarpc.lib.model.*;
import stoufexis.jarpc.lib.util.*;

import static stoufexis.jarpc.lib.util.Util.createServerSubscription;
import static stoufexis.jarpc.lib.util.Util.illegal;

import _package_.common.*;
import static _package_.common._Service_Metadata.*;

public class _Service_SingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements _Service_SingleThreadedServer, AutoCloseable {

  /// foreachType
  private final _Type_RequestHandler _type_RequestHandler;

  private final _Type_ResponseEncodeImpl _type_ResponseEncode = new _Type_ResponseEncodeImpl();

  private final _Type_RequestDecodeImpl _type_RequestDecode = new _Type_RequestDecodeImpl();

  /// foreachType

  _Service_SingleThreadedJarpcServer(
      Subscription subscription,
      ServerPublications publications,
      Images images,
      /// foreachType
      _Type_RequestHandler _type_RequestHandler,
      /// foreachType
      ServerErrorHandler errorHandler) {
    super(subscription, publications, images, errorHandler);
    /// foreachType
    this._type_RequestHandler = _type_RequestHandler;
    /// foreachType
  }

  public static _Service_SingleThreadedJarpcServer create(
      ConnectivityConfig cfg,
      /// foreachType
      _Type_RequestHandler _type_RequestHandler,
      /// foreachType
      ServerErrorHandler serverErrorHandler) {
    Images images = new Images();

    Subscription serverSubscription =
        createServerSubscription(cfg.aeron(), images, cfg.requestEndpoint(), cfg.requestStreamId());

    return new _Service_SingleThreadedJarpcServer(
        serverSubscription,
        new ServerPublications(cfg.responseControl(), cfg.aeron(), cfg.responseStreamId()),
        images,
        /// foreachType
        _type_RequestHandler,
        /// foreachType
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
    Publication publication = getPublication(clientId);

    if (publication == null) {
      claimHandle.setFailed(ErrorCode.CLIENT_NOT_EXISTS);
      return null;
    }

    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = Publisher.tryClaim(_TYPE__RESPONSE_SIZE, claim, publication);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    int newOffset = Publisher.encodeHeader(correlationId, _TYPE__MESSAGE_TYPE, claim);
    claimHandle.setSuccess(correlationId);
    _type_ResponseEncode.set(claim.buffer(), newOffset);
    return _type_ResponseEncode;
  }

  private boolean on_Type_Request(
      long clientId, long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != _TYPE__REQUEST_SIZE) {
      throw illegal("Unable to process _Type_Request");
    }

    _type_RequestDecode.set(buffer, offset);
    return _type_RequestHandler.onRequest(clientId, correlationId, _type_RequestDecode);
  }

  /// foreachType

  /// foreachType
  private static final class _Type_RequestDecodeImpl extends DecodeUtil
      implements _Type_RequestDecode {
    /// foreachRequestField
    @Override
    public _fieldType_ get_Field_() {
      return buffer.get_FieldType_(offset + _fieldOffset_);
    }
    /// foreachRequestField
  }

  private static final class _Type_ResponseEncodeImpl extends EncodeUtil
      implements _Type_ResponseEncode {
    /// foreachResponseField
    @Override
    public void set_Field_(_fieldType_ _field_) {
      buffer.put_FieldType_(offset + _fieldOffset_, _field_);
    }
    /// foreachResponseField
  }
  /// foreachType
}
