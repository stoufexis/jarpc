package stoufexis.jarpc.exchange.model;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import stoufexis.jarpc.model.Message;

public final class CancelAllResponse extends Message {
  private int statusCode;

  public CancelAllResponse() {
    super(CancelAllResponse.class, 4);
  }

  public void reset() {
    uninitialize();
    statusCode = 0;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void set(int statusCode) {
    initialize();

    this.statusCode = statusCode;
  }

  @Override
  public void decode(DirectBuffer buffer, int offset, int length) {
    checkSize(length);
    initialize();

    this.statusCode = buffer.getInt(offset);
  }

  @Override
  public void encode(MutableDirectBuffer buffer, int offset) {
    checkInitialized();

    buffer.putInt(offset, this.statusCode);
  }

  @Override
  public String toString() {
    return "CancelAllResponse{initialized=" + isInitialized() + ",statusCode=" + statusCode + "}";
  }
}
