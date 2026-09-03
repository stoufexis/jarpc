package stoufexis.sample;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.agrona.concurrent.AgentRunner;
import stoufexis.jarpc.lib.client.ClientErrorHandler;
import stoufexis.jarpc.lib.common.Bytes;
import stoufexis.jarpc.lib.common.ErrorCode;
import stoufexis.sample.generated.client.LeaseConcurrentClient.*;
import stoufexis.sample.generated.client.LeaseConcurrentJarpcClient;
import stoufexis.sample.generated.common.*;

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

  static void compete(UUID clientId, long key, LeaseConcurrentJarpcClient client)
      throws InterruptedException, ExecutionException {
    IO.println("Assigned " + clientId);

    Bytes bytes = new Bytes(16);
    ByteBuffer.wrap(bytes.backingArray())
        .putLong(clientId.getMostSignificantBits())
        .putLong(clientId.getLeastSignificantBits());

    while (true) {

      CompletableFuture<Boolean> acquiredFut = new CompletableFuture<>();

      client.acquire(
          new Acquire(key, bytes),
          new AcquireResponseHandler() {
            @Override
            public boolean onResponse(AcquireResponseDecode t) {
              acquiredFut.complete(t.acquired());
              return true;
            }

            @Override
            public boolean onClientDecodeError(long correlationId) {
              IO.println("ClientDecodeError");
              return true;
            }
          });

      boolean acquired = acquiredFut.get();

      IO.println("Acquired " + acquired);
      CompletableFuture<Void> qFut = new CompletableFuture<>();

      client.query(
          new Query(key),
          new QueryResponseHandler() {
            @Override
            public boolean onResponse(QueryResponseDecode t) {
              ByteBuffer buf = ByteBuffer.wrap(t.value().backingArray());

              IO.println("QueryResponse Owned by >>" + new UUID(buf.getLong(), buf.getLong()));

              qFut.complete(null);
              return true;
            }

            @Override
            public boolean onClientDecodeError(long correlationId) {
              IO.println("ClientDecodeError");
              return true;
            }
          });

      qFut.get();

      if (acquired) {
        for (int i = 0; i < 15; i++) {
          CompletableFuture<Void> rFut = new CompletableFuture<>();

          client.refresh(
              new Refresh(key),
              new RefreshResponseHandler() {
                @Override
                public boolean onResponse(RefreshResponseDecode t) {
                  IO.println("RefreshResponse " + t.acquired());
                  rFut.complete(null);
                  return true;
                }

                @Override
                public boolean onClientDecodeError(long correlationId) {
                  IO.println("ClientDecodeError");
                  return true;
                }
              });

          rFut.get();
          Thread.sleep(1000);
        }

        break;
      }

      Thread.sleep(2500);
    }
  }

  static void main() throws InterruptedException, ExecutionException {
    try (MediaDriver mediaDriver = Shared.mediaDriver();
        Aeron aeron = Shared.aeron(mediaDriver);
        //
        LeaseConcurrentJarpcClient client =
            LeaseConcurrentJarpcClient.create(
                Shared.connectivityConfig(aeron), new ErrorHandler(), 1024);
        //
        AgentRunner agentRunner = Shared.runner(client)) {

      AgentRunner.startOnThread(agentRunner);

      while (!client.isConnected()) {
        Thread.sleep(100);
      }

      long key = 123123123;
      UUID clientId = UUID.randomUUID();
      compete(clientId, key, client);
    }
  }
}
