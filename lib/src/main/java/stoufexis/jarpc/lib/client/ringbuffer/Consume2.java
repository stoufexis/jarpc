package stoufexis.jarpc.lib.client.ringbuffer;

public interface Consume2<A, B> {
  void accept(A a, B b);
}
