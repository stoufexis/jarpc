package stoufexis.jarpc.model;

import org.agrona.ErrorHandler;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;

public abstract class JarpcClient {

  public abstract Agent getAgent();

  public abstract ErrorHandler getHandler();

  public final Thread startOnThread(IdleStrategy idleStrategy) {
    return AgentRunner.startOnThread(new AgentRunner(idleStrategy, getHandler(), null, getAgent()));
  }
}
