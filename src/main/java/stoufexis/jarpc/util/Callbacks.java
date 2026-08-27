package stoufexis.jarpc.util;

import java.util.concurrent.atomic.AtomicReferenceArray;

public final class Callbacks<T> {
  public static final int NO_SPACE = ConcurrentIntStack.EMPTY;

  private final ConcurrentIntStack stack;
  private final AtomicReferenceArray<T> array;

  public Callbacks(int size) {
    this.stack = new ConcurrentIntStack(size);
    this.array = new AtomicReferenceArray<>(size);
  }

  // FIXME maybe we don't need a full atomic set/get on every method?
  //  Happens-before for some operations is already established by the stack.

  public int put(T callback) {
    int i = stack.pop();
    if (i == NO_SPACE) return NO_SPACE;
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
}
