package stoufexis.jarpc.client;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import stoufexis.jarpc.model.MessageHeaderCodec;
import stoufexis.jarpc.model.ProcessingFailureCodec;

import static stoufexis.jarpc.util.Util.illegal;

public abstract class PollFragmentHandler {
  private final ErrorHandler handler;
  private final ControlledFragmentHandler fragmentHandler;

  protected PollFragmentHandler(ErrorHandler handler) {
    this.handler = handler;
    this.fragmentHandler = new ControlledFragmentAssembler(this::onFragment);
  }

  public ControlledFragmentHandler getFragmentHandler() {
    return fragmentHandler;
  }

  private ControlledFragmentHandler.Action onFragment(
      DirectBuffer buffer, int offset, int length, Header header) {
    try {
      MessageHeaderCodec.assertSize(length);
      int messageType = MessageHeaderCodec.decodeMessageType(buffer, offset);
      long correlationId = MessageHeaderCodec.decodeCorrelationId(buffer, offset);

      offset += MessageHeaderCodec.HEADER_SIZE;
      length -= MessageHeaderCodec.HEADER_SIZE;

      boolean accepted;

      if (messageType > 0) {
        accepted = onMessage(messageType, correlationId, buffer, offset, length);
      } else if (messageType == ProcessingFailureCodec.MESSAGE_TYPE_ID) {
        ProcessingFailureCodec.assertSize(length);
        int baseMessageType = ProcessingFailureCodec.decodeBaseMessageType(buffer, offset);
        accepted = onProcessingFailure(baseMessageType, correlationId);
      } else {
        throw illegal("Unknown message type " + messageType);
      }

      return accepted
          ? ControlledFragmentHandler.Action.CONTINUE
          : ControlledFragmentHandler.Action.ABORT;

    } catch (RuntimeException e) {
      handler.onError(e);
      return ControlledFragmentHandler.Action.CONTINUE;
    }
  }

  protected abstract boolean onProcessingFailure(int baseMessageType, long correlationId);

  protected abstract boolean onMessage(
      int messageType, long correlationId, DirectBuffer buffer, int offset, int length);
}
