/// foreachType
package _package_.common;

import stoufexis.jarpc.lib.common.*;

public interface _Type_RequestEncode {
  /// foreachRequestField
  void set_Field_(_javaType_ _field_);

  /// foreachRequestField

  default void set(_Type_RequestDecode decode) {
    /// foreachRequestField
    set_Field_(decode._field_());
    /// foreachRequestField
  }
}
/// foreachType
