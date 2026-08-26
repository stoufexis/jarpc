package stoufexis.jarpc.exchange;

import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import stoufexis.jarpc.util.MessageHeader;
import stoufexis.jarpc.util.ResponseServer;

import static stoufexis.jarpc.util.Util.illegal;

public class JarpcExchangeServer implements ResponseServer.ResponseHandler {
  private final long clientId;
  private final ExchangeServer exchange;
  private final ErrorHandler handler;

  private final MessageHeader header = new MessageHeader();
  private final PostOrderRequest postOrderRequest = new PostOrderRequest();
  private final CancelAllRequest cancelAllRequest = new CancelAllRequest();

  public JarpcExchangeServer(ExchangeServer exchange, Image image, ErrorHandler handler) {
    this.exchange = exchange;
    this.clientId = image.correlationId();
    this.handler = handler;
  }

  @Override
  public boolean onMessage(
      DirectBuffer buffer, int offset, int length, Header h_, Publication responsePublication) {
    try {
      header.decode(buffer, offset, length);

      offset += MessageHeader.HEADER_SIZE;
      length -= MessageHeader.HEADER_SIZE;

      int messageType = header.getMessageType();
      long correlationId = header.getCorrelationId();

      switch (messageType) {
        case Catalog.postOrderId -> {
          try {

          } catch (RuntimeException e) {
            throw new RuntimeException(e);
          }
        }

        case Catalog.cancelAllOrdersId -> {
          //          ExchangeClient.CancelAllCallback callback = removeOrThrow(cancelAllCallbacks,
          // correlationId);
          //
          //          try {
          //            cancelAllResponse.decode(buffer, offset, length);
          //            return callback.onResponse(correlationId, cancelAllResponse);
          //
          //          } catch (RuntimeException e) {
          //            callback.onError(correlationId, BaseCallback.ErrorType.DECODE_ERROR, e);
          //            return true;
          //          }
        }

        default -> {
          handler.onError(illegal("Unknown message type " + messageType));
          return true;
        }
      }

    } catch (RuntimeException e) {
      handler.onError(e);
      throw e;
    }

    return false;
  }
}
