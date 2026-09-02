package stoufexis.sample;

import io.aeron.Aeron;
import stoufexis.jarpc.lib.model.ConnectivityConfig;

public final class Constants {
  public static ConnectivityConfig connectivityConfig(Aeron aeron) {
    return new ConnectivityConfig(aeron, "localhost:10000", 1, "localhost:10001", 2);
  }
}
