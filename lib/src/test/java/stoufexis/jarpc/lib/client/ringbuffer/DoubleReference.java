package stoufexis.jarpc.lib.client.ringbuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DoubleReference {
  int intA = 0;
  int intB = 0;

  @Override
  public String toString() {
    return "DoubleReference(" + intA + ", " + intB + ")";
  }

  void set(int intA, int intB) {
    this.intA = intA;
    this.intB = intB;
  }

  void copy(DoubleReference ref) {
    set(ref.intA, ref.intB);
  }

  static void assertScratch(int a, int b, DoubleReference scratch) {
    assertEquals(a, scratch.intA);
    assertEquals(b, scratch.intB);
  }

  static boolean offer(MPSCRingBuffer<DoubleReference> buf, int a, int b) {
    return buf.offer(DoubleReference::set, a, b);
  }

  static boolean poll(MPSCRingBuffer<DoubleReference> buf, DoubleReference scratch) {
    return buf.poll(DoubleReference::copy, scratch);
  }
}
