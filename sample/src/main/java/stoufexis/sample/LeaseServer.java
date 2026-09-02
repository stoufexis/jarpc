package stoufexis.sample;

import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;
import stoufexis.jarpc.lib.model.*;
import stoufexis.sample.generated.lease.common.*;
import stoufexis.sample.generated.lease.server.LeaseSingleThreadedJarpcServer;
import stoufexis.sample.generated.lease.server.LeaseSingleThreadedServer;
import stoufexis.sample.generated.lease.server.LeaseSingleThreadedStateMachine;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

public class LeaseServer implements Agent, AutoCloseable {
  private final LeaseSingleThreadedJarpcServer singleThreadedJarpcServer;
  private final StateMachine stateMachine;

  public LeaseServer(ConnectivityConfig cfg) {
    this.stateMachine = new StateMachine();
    this.singleThreadedJarpcServer = LeaseSingleThreadedJarpcServer.create(cfg, stateMachine);
  }

  @Override
  public int doWork() {
    int work = 0;
    work += stateMachine.onTick();
    work += singleThreadedJarpcServer.poll(1);
    return work;
  }

  @Override
  public String roleName() {
    return "LeaseServer";
  }

  @Override
  public void close() {
    singleThreadedJarpcServer.close();
  }

  private static final class StateMachine implements LeaseSingleThreadedStateMachine {
    private static final int POOL_CAPACITY = 1024;
    private static final int TTL_SECONDS = 10;
    private static final int TTL_NANOS = (int) TimeUnit.SECONDS.toNanos(TTL_SECONDS);
    private static final long TICK_NANOS = 100_000_000;

    private long lastTickAtNanos = Long.MIN_VALUE;
    private final ClaimHandle claimHandle = new ClaimHandle();
    private final ArrayDeque<Bytes> bytesPool = new ArrayDeque<>(1024);
    private final Long2ObjectHashMap<Bytes> payloads = new Long2ObjectHashMap<>();
    private final Long2LongHashMap owners = new Long2LongHashMap(Long.MIN_VALUE);
    private final Long2LongHashMap received = new Long2LongHashMap(Long.MIN_VALUE);

    @Override
    public boolean onRequest(
        long clientId,
        long correlationId,
        AcquireRequestDecode t,
        LeaseSingleThreadedServer server) {
      illegalClientId(clientId);

      AcquireResponseEncode encode = server.claimAcquire(clientId, correlationId, claimHandle);
      ErrorCode code = claimHandle.getCode();

      // Fail early if we cannot respond
      if (code != null) {
        couldNotRespond(code);
        return true;
      }

      long key = t.key();
      System.out.println("got acquire for " + key + " from " + clientId);

      if (payloads.containsKey(key)) {
        encode.setAcquired(false);

      } else {
        Bytes payload = getPayloadBuffer();
        payload.copy(t.value());
        put(key, payload, clientId, System.nanoTime());

        encode.setAcquired(true);
      }

      claimHandle.commit();
      return true;
    }

    @Override
    public boolean onRequest(
        long clientId, long correlationId, QueryRequestDecode t, LeaseSingleThreadedServer server) {
      illegalClientId(clientId);

      QueryResponseEncode encode = server.claimQuery(clientId, correlationId, claimHandle);
      ErrorCode code = claimHandle.getCode();

      // Fail early if we cannot respond
      if (code != null) {
        couldNotRespond(code);
        return true;
      }

      long key = t.key();
      System.out.println("got query for " + key + " from " + clientId);

      Bytes payload = payloads.get(key);

      if (payload == null) {
        encode.setExists(false);

      } else {
        long expiresInNanos = TTL_NANOS - (System.nanoTime() - received.get(key));

        encode.setExists(true);
        encode.setExpiresInSeconds((int) TimeUnit.NANOSECONDS.toSeconds(expiresInNanos));
        encode.setValue(payload);
      }

      claimHandle.commit();
      return true;
    }

    @Override
    public boolean onRequest(
        long clientId,
        long correlationId,
        RefreshRequestDecode t,
        LeaseSingleThreadedServer server) {
      illegalClientId(clientId);

      RefreshResponseEncode encode = server.claimRefresh(clientId, correlationId, claimHandle);
      ErrorCode code = claimHandle.getCode();

      // Fail early if we cannot respond
      if (code != null) {
        couldNotRespond(code);
        return true;
      }

      long key = t.key();
      System.out.println("got refresh for " + key + " from " + clientId);

      if (owners.get(key) != clientId) {
        encode.setAcquired(false);

      } else {
        received.put(key, System.nanoTime());
        encode.setAcquired(true);
      }

      claimHandle.commit();

      return true;
    }

    @Override
    public void onClientDisconnected(long clientId) {
      illegalClientId(clientId);

      for (long key : payloads.keySet()) {
        if (owners.get(key) == clientId) {
          remove(key);
          System.out.println(
              "removing " + key + " because its owner " + clientId + " disconnected");
        }
      }
    }

    int onTick() {
      int i = 0;

      long now = System.nanoTime();

      if (now - lastTickAtNanos > TICK_NANOS) {
        lastTickAtNanos = now;

        for (long key : payloads.keySet()) {
          if (now - received.get(key) > TTL_NANOS) {
            remove(key);
            System.out.println("key " + key + " expired");
            i++;
          }
        }
      }

      return i;
    }

    @Override
    public void onInternalError(long clientId, long correlationId, int errorCode) {
      illegalClientId(clientId);
      System.out.println("InternalError " + clientId + ", " + correlationId + ", " + errorCode);
    }

    @Override
    public void onProcessingError(long clientId, long correlationId, int messageType) {
      illegalClientId(clientId);
      System.out.println("ProcessingError " + clientId + ", " + correlationId);
    }

    @Override
    public void onError(Throwable throwable) {
      System.out.println(throwable.toString());
    }

    @Override
    public void onCorruptPublication(ErrorCode code) {
      System.out.println("CorruptPublication " + code);
    }

    private void remove(long key) {
      Bytes payload = payloads.remove(key);
      owners.remove(key);
      received.remove(key);

      // return to the pool, so future gets can avoid allocation
      if (payload != null && bytesPool.size() < POOL_CAPACITY) {
        bytesPool.add(payload);
      }
    }

    private void put(long key, Bytes payload, long clientId, long receivedAt) {
      payloads.put(key, payload);
      owners.put(key, clientId);
      received.put(key, receivedAt);
    }

    private Bytes getPayloadBuffer() {
      if (bytesPool.isEmpty()) {
        return new Bytes(16);
      } else {
        return bytesPool.getFirst();
      }
    }

    private void illegalClientId(long clientId) {
      if (clientId == Long.MIN_VALUE)
        throw new IllegalArgumentException("Client id clashes with sentinel");
    }

    private void couldNotRespond(ErrorCode code) {
      System.out.println("Could not deliver response " + code);
    }
  }
}
