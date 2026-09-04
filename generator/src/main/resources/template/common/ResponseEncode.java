/// foreachType
package _package_.common;

import stoufexis.jarpc.lib.common.*;

public interface _Type_ResponseEncode {
  /// foreachResponseField
  void set_Field_(_javaType_ _field_);
  /// foreachResponseField

  default void set(_Type_ResponseDecode decode) {
    /// foreachResponseField
    set_Field_(decode._field_());
    /// foreachResponseField
  }
}
/// foreachType
