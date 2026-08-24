package stoufexis.jarpc;

import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static java.lang.IO.println;

// TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

  static void main() {
    ByteBuffer bb = ByteBuffer.allocate(8); // default order: big-endian
    UnsafeBuffer ub = new UnsafeBuffer(bb); // default order: native

    ub.putInt(0, 42); // written native (little-endian on x86)
    int x = ub.getInt(0); // read big-endian -> byte-swapped, not 42
    println(x);
  }
}
