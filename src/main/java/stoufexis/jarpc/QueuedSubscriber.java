package stoufexis.jarpc;

import io.aeron.ControlledFragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

// Only allows a single consumer thread at a time
final class QueuedSubscriber<T> {
  private final RingBuffer buf;
  private final Agent agent;
  private final SubscriberMessageHandler<T> handler;

  QueuedSubscriber(
      Decoder<T> decoder, Subscription sub, int capacity, SubscriberConsumer<T> consumer) {
    this.buf =
        new OneToOneRingBuffer(
            new UnsafeBuffer(
                ByteBuffer.allocateDirect(capacity + RingBufferDescriptor.TRAILER_LENGTH)));
    this.agent = new ReceiveAgent(sub, this.buf);
    this.handler = new SubscriberMessageHandler<>(consumer, decoder);
  }

  Agent agent() {
    return agent;
  }

  void poll() {
    buf.controlledRead(handler);
  }

  interface SubscriberConsumer<T> {
    boolean consume(T elem);
  }

  private static final class SubscriberMessageHandler<T> implements ControlledMessageHandler {
    private final SubscriberConsumer<T> consumer;
    private final Decoder<T> decoder;

    private SubscriberMessageHandler(SubscriberConsumer<T> consumer, Decoder<T> decoder) {
      this.consumer = consumer;
      this.decoder = decoder;
    }

    @Override
    public Action onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
      return consumer.consume(decoder.decode(index, length, buffer))
          ? Action.CONTINUE
          : Action.ABORT;
    }
  }

  private static final class ReceiveAgent implements Agent, ControlledFragmentHandler {
    private static final int BATCH_LIMIT = 10;

    private final Subscription sub;
    private final ControlledFragmentHandler assembled;
    private final RingBuffer buf;

    private ReceiveAgent(Subscription sub, RingBuffer buf) {
      this.sub = sub;
      this.buf = buf;
      this.assembled = new ControlledFragmentAssembler(this);
    }

    @Override
    public int doWork() {
      return sub.controlledPoll(assembled, BATCH_LIMIT);
    }

    @Override
    public Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
      int index = buf.tryClaim(0, length);

      if (index <= 0) {
        return Action.ABORT;
      }

      AtomicBuffer underlying = buf.buffer();
      underlying.putBytes(index, buffer, offset, length);
      buf.commit(index);
      return Action.CONTINUE;
    }

    @Override
    public String roleName() {
      return this.getClass().getName();
    }
  }
}
