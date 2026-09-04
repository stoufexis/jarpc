package _package_.client;

import io.aeron.Publication;
import io.aeron.Subscription;

import org.agrona.DirectBuffer;

import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.common.*;

import static stoufexis.jarpc.lib.common.Util.*;

import static _package_.common._Service_Metadata.*;
import _package_.common.*;

public final class _Service_SingleThreadedJarpcClient extends SingleThreadedJarpcClient
    implements _Service_SingleThreadedClient {
  /// foreachType
  private final _Type_RequestEncodeImpl _type_RequestEncode = new _Type_RequestEncodeImpl();
  private final _Type_ResponseDecodeImpl _type_ResponseDecode = new _Type_ResponseDecodeImpl();
  private final _Type_ResponseHandler _type_ResponseHandler;
  /// foreachType
  _Service_SingleThreadedJarpcClient(
      Publication publication,
      Subscription subscription,
      /// foreachType
      _Type_ResponseHandler _type_Handler,
      /// foreachType
      ClientErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);
    /// foreachType
    this._type_ResponseHandler = _type_Handler;
    /// foreachType
  }

  public static _Service_SingleThreadedJarpcClient create(
      ConnectionConfig cfg,
      /// foreachType
      _Type_ResponseHandler _type_Handler,
      /// foreachType
      ClientErrorHandler errorHandler) {
    Subscription sub =
        createClientSubscription(cfg.aeron(), cfg.responseControl(), cfg.responseStreamId());
    Publication pub =
        createExclusiveClientPublication(
            cfg.aeron(), cfg.requestEndpoint(), cfg.requestStreamId(), sub);
    return new _Service_SingleThreadedJarpcClient(
        pub,
        sub,
        /// foreachType
        _type_Handler,
        /// foreachType
        errorHandler);
  }

  @Override
  protected boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length) {
    return switch (messageType) {
      /// foreachType
      case _TYPE__MESSAGE_TYPE -> on_Type_Response(correlationId, buffer, offset, length);
      /// foreachType
      default -> throw illegal("Unknown message type " + messageType);
    };
  }

  /// foreachType
  @Override
  public _Type_RequestEncode claim_Type_(ClaimHandle claimHandle) {

    publisher.tryClaim(_TYPE__REQUEST_SIZE, claimHandle);
    if (claimHandle.isFailed()) return null;
    _type_RequestEncode.set(claimHandle, publisher.encodeHeader(_TYPE__MESSAGE_TYPE, claimHandle));
    return _type_RequestEncode;
  }

  private boolean on_Type_Response(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length != _TYPE__RESPONSE_SIZE) {
      return _type_ResponseHandler.onClientDecodeError(correlationId);
    }
    _type_ResponseDecode.set(buffer, offset);
    return _type_ResponseHandler.onResponse(correlationId, _type_ResponseDecode);
  }

  private static final class _Type_RequestEncodeImpl extends EncodeUtil
      implements _Type_RequestEncode {
    /// foreachRequestField
    @Override
    public void set_Field_(_javaType_ _field_) {
      put_FieldType_(_fieldOffset_, _field_);
    }
    /// foreachRequestField
  }

  private static final class _Type_ResponseDecodeImpl extends DecodeUtil
      implements _Type_ResponseDecode {
    /// foreachResponseField
    @Override
    public _javaType_ _field_() {
      return get_FieldType_(_fieldOffset_);
    }
    /// foreachResponseField
  }
  /// foreachType
}
