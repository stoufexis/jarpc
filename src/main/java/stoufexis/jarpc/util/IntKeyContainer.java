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

  public T remove(int i) {
    T callback = array.getAndSet(i, null);
    if (callback == null) return null;

    stack.push(i);
    return callback;
  }

  public T get(int i) {
    return array.get(i);
  }

  public T fetchOrThrow(int key, boolean remove) {
    T value;
    if (remove) {
      value = remove(key);
    } else {
      value = get(key);
    }
    if (value == null) throw illegal("Callback not registered for correlation id " + key);
    return value;
  }
}
