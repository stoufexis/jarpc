package stoufexis.jarpc.model;

import org.agrona.ErrorHandler;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;

public interface JarpcClient {

  Agent getAgent();

  ErrorHandler getHandler();

  default Thread startOnThread(IdleStrategy idleStrategy) {
    return AgentRunner.startOnThread(
        new AgentRunner(idleStrategy, getHandler(), null, getAgent()));
  }
}
