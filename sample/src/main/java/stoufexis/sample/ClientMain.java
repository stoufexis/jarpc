package stoufexis.sample;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.agrona.concurrent.AgentRunner;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.common.Bytes;
import stoufexis.jarpc.lib.common.ErrorCode;
import stoufexis.sample.generated.client.LeaseConcurrentClient.*;
import stoufexis.sample.generated.client.LeaseConcurrentJarpcClient;
import stoufexis.sample.generated.common.*;

// This client implementation freely allocates to keep it simple. 0-allocation usage is possible
// even with the concurrent client implementation. Usage of the single threaded client is not shown
// here.

public final class ClientMain {
  static class ErrorHandler implements ClientErrorHandler {
    @Override
    public void onCallbackNotFound(long correlationId, String type) {
      IO.println("Callback not found " + type);
    }

    @Override
    public void onCorruptPublication(ErrorCode code) {
      IO.println("Corrupt publication " + code);
    }

    @Override
    public void onError(Throwable throwable) {
      IO.println(throwable.toString());
    }
  }

  record Acquire(long key, Bytes value) implements AcquireRequestDecode {}

  record Query(long key) implements QueryRequestDecode {}

  record Refresh(long key) implements RefreshRequestDecode {}

  static CompletableFuture<Boolean> acquire(LeaseConcurrentJarpcClient client, long key, UUID id) {
    CompletableFuture<Boolean> fut = new CompletableFuture<>();

    Bytes bytes = new Bytes(16);
    ByteBuffer.wrap(bytes.backingArray())
        .putLong(id.getMostSignificantBits())
        .putLong(id.getLeastSignificantBits());

    client.acquire(
        new Acquire(key, bytes),
        new AcquireResponseHandler() {
          @Override
          public boolean onResponse(AcquireResponseDecode t) {
            fut.complete(t.acquired());
            return true;
          }

          @Override
          public boolean onClientDecodeError(long correlationId) {
            IO.println("ClientDecodeError");
            return true;
          }
        });

    return fut;
  }

  static CompletableFuture<UUID> query(LeaseConcurrentJarpcClient client, long key) {
    CompletableFuture<UUID> fut = new CompletableFuture<>();

    client.query(
        new Query(key),
        new QueryResponseHandler() {
          @Override
          public boolean onResponse(QueryResponseDecode t) {
            if (t.exists()) {
              ByteBuffer buf = ByteBuffer.wrap(t.value().backingArray());
              fut.complete(new UUID(buf.getLong(), buf.getLong()));
            } else {
              fut.complete(null);
            }
            return true;
          }

          @Override
          public boolean onClientDecodeError(long correlationId) {
            fut.completeExceptionally(new RuntimeException("Client decode error"));
            return true;
          }
        });

    return fut;
  }

  static CompletableFuture<Boolean> refresh(LeaseConcurrentJarpcClient client, long key) {
    CompletableFuture<Boolean> rFut = new CompletableFuture<>();

    client.refresh(
        new Refresh(key),
        new RefreshResponseHandler() {
          @Override
          public boolean onResponse(RefreshResponseDecode t) {
            rFut.complete(t.acquired());
            return true;
          }

          @Override
          public boolean onClientDecodeError(long correlationId) {
            rFut.completeExceptionally(new RuntimeException("Client decode error"));
            return true;
          }
        });

    return rFut;
  }

  static void compete(UUID clientId, long key, LeaseConcurrentJarpcClient client)
      throws InterruptedException, ExecutionException, TimeoutException {
    IO.println("Assigned " + clientId);

    while (true) {

      boolean acquired = acquire(client, key, clientId).get(1, TimeUnit.SECONDS);

      IO.println("Acquired " + acquired);

      UUID queryResponse = query(client, key).get(1, TimeUnit.SECONDS);

      IO.println("QueryResponse Owned by >>" + queryResponse);

      if (acquired) {

        for (int i = 0; i < 15; i++) {
          boolean refreshed = refresh(client, key).get(1, TimeUnit.SECONDS);
          IO.println("Refreshed " + refreshed);

          Thread.sleep(1000);
        }

        break;
      }

      Thread.sleep(2500);
    }
  }

  static void main() throws InterruptedException, ExecutionException, TimeoutException {
    try (MediaDriver mediaDriver = Shared.mediaDriver();
        Aeron aeron = Shared.aeron(mediaDriver);
        //
        LeaseConcurrentJarpcClient client =
            LeaseConcurrentJarpcClient.create(aeron, Shared.connection, new ErrorHandler(), 1024);
        //
        AgentRunner agentRunner = Shared.runner(client)) {

      AgentRunner.startOnThread(agentRunner);

      while (!client.isConnected()) Thread.sleep(100);

      long key = 123123123;
      UUID clientId = UUID.randomUUID();
      compete(clientId, key, client);
    }
  }
}
