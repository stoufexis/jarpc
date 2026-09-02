/// foreachType
package _package_.common;

import stoufexis.jarpc.lib.model.*;

public interface _Type_RequestEncode {
  /// foreachRequestField
  void set_Field_(_javaType_ _field_);

  /// foreachRequestField

  default void set(_Type_RequestDecode decode) {
    /// foreachRequestField
    set_Field_(decode.get_Field_());
    /// foreachRequestField
  }
}
/// foreachType
