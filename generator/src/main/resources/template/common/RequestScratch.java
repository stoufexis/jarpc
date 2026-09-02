/// foreachType
package _package_.common;

import stoufexis.jarpc.lib.model.*;
import _package_.client._Service_ConcurrentClient;

public final class _Type_RequestScratch implements _Type_RequestDecode, _Type_RequestEncode {
  /// foreachRequestField
  private _javaType_ _field_ = _initialValue_;

  /// foreachRequestField
  private _Service_ConcurrentClient._Type_ResponseHandler handler;

  public static void copy(_Type_RequestScratch scratch1, _Type_RequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      _Type_RequestScratch scratch,
      _Type_RequestDecode decode,
      _Service_ConcurrentClient._Type_ResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public _Service_ConcurrentClient._Type_ResponseHandler getHandler() {
    return handler;
  }

  /// foreachRequestField
  @Override
  public _javaType_ get_Field_() {
    return _field_;
  }

  @Override
  public void set_Field_(_javaType_ _field_) {
    _copy_;
  }

  /// foreachRequestField

  public void setHandler(_Service_ConcurrentClient._Type_ResponseHandler handler) {
    this.handler = handler;
  }
}
/// foreachType
