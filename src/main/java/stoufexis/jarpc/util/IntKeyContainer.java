package stoufexis.jarpc.util;

import stoufexis.jarpc.model.ErrorCode;

import java.util.concurrent.atomic.AtomicReferenceArray;

import static stoufexis.jarpc.util.Util.illegal;

public final class IntKeyContainer<T> {
  private final ConcurrentIntStack stack;
  private final AtomicReferenceArray<T> array;

  public IntKeyContainer(int size) {
    this.stack = new ConcurrentIntStack(size);
    this.array = new AtomicReferenceArray<>(size);
  }

  // FIXME maybe we don't need a full atomic set/get on every method?
  //  Happens-before for some operations is already established by the stack.

  public int put(T callback) {
    int i = stack.pop();
    if (i == ErrorCode.BACKPRESSURE) return ErrorCode.BACKPRESSURE;
    array.set(i, callback);
    return i;
  }

  public void remove(int i) {
    if (array.getAndSet(i, null) != null) stack.push(i);
  }

  public T get(int i) {
    return array.get(i);
  }

  public T getOrThrow(int key) {
    T value = get(key);
    if (value == null) throw illegal("Callback not registered for correlation id " + key);
    return value;
  }
}
