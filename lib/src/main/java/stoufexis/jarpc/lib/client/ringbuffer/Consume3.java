package stoufexis.jarpc.lib.client.ringbuffer;

public interface Consume3<A, B, C> {
  void accept(A a, B b, C c);
}
