package stoufexis.jarpc.util;

final class ConcurrentIntStack {
  static final int EMPTY = -1;

  private final int size;

  ConcurrentIntStack(int size) {
    this.size = size;
  }

  int pop() {
    throw new UnsupportedOperationException();
  }

  void push(int i) {
    throw new UnsupportedOperationException();
  }
}
