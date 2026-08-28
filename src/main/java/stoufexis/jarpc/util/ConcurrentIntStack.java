package stoufexis.jarpc.util;

import static stoufexis.jarpc.util.Util.illegal;

// [1,2,3,4,5]
//    |
final class ConcurrentIntStack {
  private int headPos = 0;
  private final int[] ints;

  ConcurrentIntStack(int size) {
    this.ints = new int[size];
    for (int i = 1; i <= size; i++) {
      ints[i] = i;
    }
  }

  synchronized int pop() {
    return ints[headPos++];
  }

  synchronized void push(int i) {
    if (headPos <= 0) throw illegal("Pushed into a full stack");
    ints[--headPos] = i;
  }
}
