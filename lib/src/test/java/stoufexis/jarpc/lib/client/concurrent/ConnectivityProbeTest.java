package stoufexis.jarpc.lib.client.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ConnectivityProbeTest {

  @Test
  void probe_detects_connected() {
    var probe = new ConnectivityProbe(() -> true);

    assertFalse(probe.isConnected());

    probe.probeConnected();
    assertTrue(probe.isConnected());

    probe.probeConnected();
    assertTrue(probe.isConnected());
  }

  @Test
  void stops_probing_once_connected() {
    // atomic is not necessary, this is used simply as a mutable long reference to pass in the probe
    AtomicInteger count = new AtomicInteger(0);
    var probe = new ConnectivityProbe(() -> count.incrementAndGet() >= 3);

    assertFalse(probe.isConnected());
    assertEquals(0, count.get());

    probe.probeConnected();
    assertFalse(probe.isConnected());
    assertEquals(1, count.get());

    probe.probeConnected();
    assertFalse(probe.isConnected());
    assertEquals(2, count.get());

    probe.probeConnected();
    assertTrue(probe.isConnected());
    assertEquals(3, count.get());

    probe.probeConnected();
    assertTrue(probe.isConnected());
    assertEquals(3, count.get());

    probe.probeConnected();
    assertTrue(probe.isConnected());
    assertEquals(3, count.get());
  }
}
