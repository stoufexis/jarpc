/// foreachType
package _package_.common;

import _package_.client.ConcurrentExchangeClient;

public final class _Type_RequestScratch implements _Type_RequestDecode, _Type_RequestEncode {

  /// foreachRequestField
  private _fieldType_ _field_;

  /// foreachRequestField

  private ConcurrentExchangeClient._Type_ResponseHandler handler;

  public static void copy(_Type_RequestScratch scratch1, _Type_RequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      _Type_RequestScratch scratch,
      _Type_RequestDecode decode,
      ConcurrentExchangeClient._Type_ResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public ConcurrentExchangeClient._Type_ResponseHandler getHandler() {
    return handler;
  }

  /// foreachRequestField
  @Override
  public _fieldType_ get_Field_() {
    return _field_;
  }

  @Override
  public void set_Field_(_fieldType_ _field_) {
    this._field_ = _field_;
  }

  /// foreachRequestField

  public void setHandler(ConcurrentExchangeClient._Type_ResponseHandler handler) {
    this.handler = handler;
  }
}
/// foreachType
