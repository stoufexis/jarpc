package stoufexis.jarpc.util;

import org.agrona.concurrent.Agent;

public abstract class ClassAgent implements Agent {
  @Override
  public final String roleName() {
    return getClass().getName();
  }
}
