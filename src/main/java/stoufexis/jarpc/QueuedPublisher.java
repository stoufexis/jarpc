package stoufexis.jarpc;

import io.aeron.Publication;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

final class QueuedPublisher<T> {
  private final Encoder<T> encoder;
  private final SendAgent agent;
  private final RingBuffer buf;

  QueuedPublisher(Encoder<T> encoder, Publication publication, int capacity) {
    this.encoder = encoder;
    this.buf =
        new ManyToOneRingBuffer(
            new UnsafeBuffer(
                ByteBuffer.allocateDirect(capacity + RingBufferDescriptor.TRAILER_LENGTH)));
    this.agent = new SendAgent(publication, this.buf);
  }

  Agent agent() {
    return this.agent;
  }

  boolean enqueue(T elem) {
    int index = buf.tryClaim(0, encoder.length(elem));

    if (index <= 0) {
      return false;
    }

    try {
      AtomicBuffer underlying = buf.buffer();
      encoder.encode(index, elem, underlying);
      buf.commit(index);
      return true;
    } catch (Exception e) {
      buf.abort(index);
      throw e;
    }
  }

  private static final class SendAgent implements Agent {
    private final RingBuffer buf;
    private final Publication pub;

    // Hoisted to avoid allocating on every doWork
    private final ControlledMessageHandler handler = this::onMessage;

    private Flag flag;

    SendAgent(Publication publication, RingBuffer buf) {
      this.pub = publication;
      this.buf = buf;
    }

    @Override
    public int doWork() {
      flag = Flag.NORMAL;

      int processed = buf.controlledRead(handler);

      return switch (flag) {
        case NORMAL -> processed;
        // always adds 1 work point for backoff. it idles even if 0 are processed
        case BACKOFF -> processed + 1;
        // FIXME needs logging
        case TERMINATE -> throw new AgentTerminationException();
      };
    }

    private ControlledMessageHandler.Action onMessage(
        int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
      return switch (pub.offer(buffer, index, length)) {
        case long i when i > 0 -> ControlledMessageHandler.Action.CONTINUE;

        case Publication.NOT_CONNECTED, Publication.BACK_PRESSURED, Publication.ADMIN_ACTION -> {
          flag = Flag.BACKOFF;
          yield ControlledMessageHandler.Action.ABORT;
        }

        case Publication.CLOSED, Publication.MAX_POSITION_EXCEEDED -> {
          flag = Flag.TERMINATE;
          yield ControlledMessageHandler.Action.ABORT;
        }

        default -> throw new IllegalStateException("Impossible offer result");
      };
    }

    @Override
    public String roleName() {
      return this.getClass().getName();
    }
  }

  private enum Flag {
    NORMAL,
    TERMINATE,
    BACKOFF
  }
}
