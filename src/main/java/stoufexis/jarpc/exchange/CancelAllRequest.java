package stoufexis.jarpc.exchange;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.Message;

public class CancelAllRequest extends Message {

  public CancelAllRequest() {
    super(CancelAllRequest.class, 0);
  }

  public void reset() {
    uninitialize();
  }

  public void set(int statusCode) {
    initialize();
  }

  @Override
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    initialize();
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();
  }
}
