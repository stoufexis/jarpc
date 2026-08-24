package stoufexis.jarpc;

import io.aeron.Publication;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

/** Allows multiple producer threads to enqueue, as it uses a MPSC buffer underneath */
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

  /**
   * Reads and releases elem in the calling thread, callers may mutate elem and call enqueue again
   * with the mutated value to reduce allocations.
   *
   * <p>Caveat; this assumes the encoder also does not capture the elem internally.
   */
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

  private static final class SendAgent implements Agent, ControlledMessageHandler {
    private final RingBuffer buf;
    private final Publication pub;

    private Flag flag;

    SendAgent(Publication publication, RingBuffer buf) {
      this.pub = publication;
      this.buf = buf;
    }

    @Override
    public int doWork() {
      flag = Flag.NORMAL;

      int processed = buf.controlledRead(this);

      return switch (flag) {
        case NORMAL -> processed;
        // always adds 1 work point for backoff. it idles even if 0 are processed
        case BACKOFF -> processed + 1;
        // FIXME needs logging
        case TERMINATE -> throw new AgentTerminationException();
      };
    }

    @Override
    public Action onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
      return switch (pub.offer(buffer, index, length)) {
        case long i when i > 0 -> Action.CONTINUE;

        case Publication.NOT_CONNECTED, Publication.BACK_PRESSURED, Publication.ADMIN_ACTION -> {
          flag = Flag.BACKOFF;
          yield Action.ABORT;
        }

        case Publication.CLOSED, Publication.MAX_POSITION_EXCEEDED -> {
          flag = Flag.TERMINATE;
          yield Action.ABORT;
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
