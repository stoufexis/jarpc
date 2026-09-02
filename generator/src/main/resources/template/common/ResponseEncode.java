/// foreachType
package _package_.common;

public interface _Type_ResponseEncode {
  /// foreachResponseField
  void set_Field_(_javaType_ _field_);

  /// foreachResponseField

  default void set(_Type_ResponseDecode decode) {
    /// foreachResponseField
    set_Field_(decode.get_Field_());
    /// foreachResponseField
  }
}
/// foreachType
