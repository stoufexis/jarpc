/// foreachType
package _package_.common;

public final class _Type_ResponseScratch implements _Type_ResponseDecode, _Type_ResponseEncode {
  /// foreachResponseField
  private _fieldType_ _field_;

  /// foreachResponseField

  public static void copy(_Type_ResponseScratch scratch1, _Type_ResponseScratch scratch2) {
    setter(scratch1, scratch2, scratch2.clientId, scratch2.correlationId);
  }

  public static void setter(
      _Type_ResponseScratch scratch,
      _Type_ResponseDecode decode,
      long clientId,
      long correlationId) {
    scratch.set(decode);
    scratch.setClientId(clientId);
    scratch.setCorrelationId(correlationId);
  }

  /// foreachResponseField
  @Override
  public _fieldType_ get_Field_() {
    return _field_;
  }

  @Override
  public void set_Field_(_fieldType_ _field_) {
    this._field_ = _field_;
  }
  /// foreachResponseField
}
/// foreachType
