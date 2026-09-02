package _package_.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.BufferClaim;

import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;

import stoufexis.jarpc.lib.util.*;
import stoufexis.jarpc.lib.client.*;
import stoufexis.jarpc.lib.model.*;

import static stoufexis.jarpc.lib.util.Util.*;

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
      ErrorHandler errorHandler) {
    super(publication, subscription, errorHandler);
    /// foreachType
    this._type_ResponseHandler = _type_Handler;
    /// foreachType
  }

  public static _Service_SingleThreadedJarpcClient create(
      ConnectivityConfig cfg,
      /// foreachType
      _Type_ResponseHandler _type_Handler,
      /// foreachType
      ErrorHandler errorHandler) {
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
    BufferClaim claim = claimHandle.getClaim();
    ErrorCode code = publisher.tryClaim(_TYPE__REQUEST_SIZE, claim);

    if (code != null) {
      claimHandle.setFailed(code);
      return null;
    }

    long id = publisher.nextCorrelationId();
    int newOffset = Publisher.encodeHeader(id, _TYPE__MESSAGE_TYPE, claim);
    claimHandle.setSuccess(id);
    _type_RequestEncode.set(claim.buffer(), newOffset);
    return _type_RequestEncode;
  }

  private boolean on_Type_Response(
      long correlationId, DirectBuffer buffer, int offset, int length) {

    if (length == _TYPE__RESPONSE_SIZE) {
      _type_ResponseDecode.set(buffer, offset);
      return _type_ResponseHandler.onResponse(correlationId, _type_ResponseDecode);

    } else {
      return _type_ResponseHandler.onClientDecodeError(correlationId);
    }
  }

  private static final class _Type_RequestEncodeImpl extends EncodeUtil
      implements _Type_RequestEncode {
    /// foreachRequestField
    @Override
    public void set_Field_(_javaType_ _field_) {
      buffer.put_FieldType_(_fieldOffset_, _field_);
    }
    /// foreachRequestField
  }

  private static final class _Type_ResponseDecodeImpl extends DecodeUtil
      implements _Type_ResponseDecode {
    /// foreachResponseField
    @Override
    public _javaType_ get_Field_(_getFieldParam_) {
      return buffer.get_FieldType_(_fieldOffset_ _getFieldParamTarget_);
    }
    /// foreachResponseField
  }
  /// foreachType
}
