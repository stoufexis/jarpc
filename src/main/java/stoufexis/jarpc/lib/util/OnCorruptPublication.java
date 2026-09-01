package stoufexis.jarpc.lib.util;

import stoufexis.jarpc.lib.model.ErrorCode;

public interface OnCorruptPublication {
  void onCorruptPublication(ErrorCode code);
}
