package stoufexis.jarpc.exchange.common;

public final class Metadata {
  private Metadata() {}

  public static final int POST_ORDER_REQUEST_SIZE = 32;
  public static final int CANCEL_ALL_REQUEST_SIZE = 0;
  public static final int POST_ORDER_RESPONSE_SIZE = 4;
  public static final int CANCEL_ALL_RESPONSE_SIZE = 4;
  public static final int POST_ORDER_MESSAGE_TYPE = 1;
  public static final int CANCEL_ALL_MESSAGE_TYPE = 2;
}
