# Jarpc

Jarpc is a performance-focused RPC library for Java. It is based on [Aeron](https://aeron.io/docs/)
and [Agrona](https://aeron.io/docs/agrona/overview/) to provide the best possible latency and throughput achievable in
the JVM, and introduces no steady-state allocations.

## State of the Project

This project currently contains a Proof of Concept for jarpc. It is completely functional, but limited in features,
under-documented, under-tested, and under-benchmarked. Additionally, it is not published as a library in any repository.

## Schema

A jarpc service is specified using a spec file. It defines each of the service's supported actions and the shape of its
request/response payloads.

Here is an example, taken from the [sample](./sample/src/main/resources/lease.json) project.

```json
[
  {
    "rpcName": "acquire",
    "request": [
      {
        "name": "key",
        "type": "long"
      },
      {
        "name": "value",
        "type": "bytes16"
      }
    ],
    "response": [
      {
        "name": "acquired",
        "type": "boolean"
      }
    ]
  },
  {
    "rpcName": "refresh",
    "request": [
      {
        "name": "key",
        "type": "long"
      }
    ],
    "response": [
      {
        "name": "acquired",
        "type": "boolean"
      }
    ]
  },
  {
    "rpcName": "query",
    "request": [
      {
        "name": "key",
        "type": "long"
      }
    ],
    "response": [
      {
        "name": "exists",
        "type": "boolean"
      },
      {
        "name": "value",
        "type": "bytes16"
      },
      {
        "name": "expiresInSeconds",
        "type": "int"
      }
    ]
  }
]
```

This service defines a lease store, where clients can acquire, hold, and refresh a lease on an arbitrary long key, while
also placing a payload while they hold it. The following types are supported:

* `boolean`
* `byte`
* `short`
* `int`
* `float`
* `long`
* `double`
* `bytes16`
* `bytes32`
* `bytes64`

## Code Generation

Given a spec file, the [code generator](./generator) creates all required classes;

* Interfaces defining the client and server
* Two client implementations; one that is thread-safe and usable across many programming paradigms, and a
  single-threaded implementation that should be used within the duty cycle of a single-threaded agent
  (e.g. [Agrona Agents](https://aeron.io/docs/agrona/agents-idle-strategies/))
* One server implementation, which is parameterized by the user's logic. It is built similarly to the single-threaded
  client implementation and is also meant to be used within the duty cycle of a single-threaded agent
* Classes that implement the encoding and decoding logic for the types defined in the spec.

You can find such a generated output in the [sample project](./sample/src/main/java/stoufexis/sample/generated). These
were created by running the following commands:

```bash
> ./gradlew :generator:installDist

> ./generator/build/install/generator/bin/generator \
    --spec ./sample/src/main/resources/lease.json \
    --name lease \
    --package 'stoufexis.sample.generated' \
    --output './sample/src/main/java/stoufexis/sample/generated/'
```

Note that the generated code depends on `stoufexis.jarpc.lib`, as it heavily uses utilities from that library to reduce
the output size.

Following are some highlights of the generated output.
See [sample project](./sample/src/main/java/stoufexis/sample/generated) for the full code.

### Lease Concurrent Client Interface

The concurrent client supports 2 usage modes, depending on garbage-tolerance:

* Allocate a fresh request and callback object for each call
* Define a re-usable class (potentially in thread-local) for the request and a singleton callback object. Correlation
  between request/response can then be done via an application-level field passed in the request and echoed in the
  response.

```java
public interface LeaseConcurrentClient {

  boolean acquire(AcquireRequestDecode request, AcquireResponseHandler response);

  interface AcquireResponseHandler extends OnClientDecodeError {
    boolean onResponse(AcquireResponseDecode t);
  }

  boolean refresh(RefreshRequestDecode request, RefreshResponseHandler response);

  interface RefreshResponseHandler extends OnClientDecodeError {
    boolean onResponse(RefreshResponseDecode t);
  }

  boolean query(QueryRequestDecode request, QueryResponseHandler response);

  interface QueryResponseHandler extends OnClientDecodeError {
    boolean onResponse(QueryResponseDecode t);
  }
}

```

### Lease Single Threaded Client Interface

A unique correlation id is issued for each request which can be used to associate it with its response

```java
public interface LeaseSingleThreadedClient extends Poll {

  AcquireRequestEncode claimAcquire(ClaimHandle claimHandle);

  interface AcquireResponseHandler extends OnClientDecodeError {
    boolean onResponse(long correlationId, AcquireResponseDecode t);
  }

  RefreshRequestEncode claimRefresh(ClaimHandle claimHandle);

  interface RefreshResponseHandler extends OnClientDecodeError {
    boolean onResponse(long correlationId, RefreshResponseDecode t);
  }

  QueryRequestEncode claimQuery(ClaimHandle claimHandle);

  interface QueryResponseHandler extends OnClientDecodeError {
    boolean onResponse(long correlationId, QueryResponseDecode t);
  }
}

```

### Lease Concurrent Client Implementation

It exposes an Agent, which drives the client's progress. The agent must be scheduled in the background with an
AgentRunner.

```java
public final class LeaseConcurrentJarpcClient
    implements LeaseConcurrentClient, Agent, AutoCloseable, IsConnected {
  // -- Output omitted for brevity -- //

}
```

### Lease Single Threaded Client Implementation

```java
public final class LeaseSingleThreadedJarpcClient extends SingleThreadedJarpcClient
    implements LeaseSingleThreadedClient {
  // -- Output omitted for brevity -- //
}
```

### Lease Single Threaded Server Interface

Each request is identified by the clientId, correlationId pair. To respond to a particular request, this pair must be
given to the claim call.

```java
public interface LeaseSingleThreadedServer extends Poll {

  AcquireResponseEncode claimAcquire(long clientId, long correlationId, ClaimHandle claimHandle);

  interface AcquireRequestHandler {
    boolean onRequest(
        long clientId,
        long correlationId,
        AcquireRequestDecode t,
        LeaseSingleThreadedServer server);
  }

  RefreshResponseEncode claimRefresh(long clientId, long correlationId, ClaimHandle claimHandle);

  interface RefreshRequestHandler {
    boolean onRequest(
        long clientId,
        long correlationId,
        RefreshRequestDecode t,
        LeaseSingleThreadedServer server);
  }

  QueryResponseEncode claimQuery(long clientId, long correlationId, ClaimHandle claimHandle);

  interface QueryRequestHandler {
    boolean onRequest(
        long clientId, long correlationId, QueryRequestDecode t, LeaseSingleThreadedServer server);
  }
}

```

### Lease Single Threaded Server Implementation

```java
public class LeaseSingleThreadedJarpcServer extends SingleThreadedJarpcServer
    implements LeaseSingleThreadedServer, AutoCloseable {
  // -- Output omitted for brevity -- //
}

```

### Encode/Decode interfaces

These interfaces model writing/reading the fields of each defined data type. Internally, these either function as
wrappers for agrona direct buffers, or act as temporary intermediate containers. They are allocated once and re-used,
never allocated per-request. When an instance is exposed to the user, it must be used in-place or copied in-place, never
stored or passed to other threads directly.

```java
public interface QueryResponseEncode {

  void setExists(boolean exists);

  void setValue(Bytes value);

  void setExpiresInSeconds(int expiresInSeconds);

  default void set(QueryResponseDecode decode) {
    setExists(decode.exists());

    setValue(decode.value());

    setExpiresInSeconds(decode.expiresInSeconds());
  }
}
```

```java
public interface QueryResponseDecode {
  boolean exists();

  Bytes value();

  int expiresInSeconds();
}
```

### Scratch Classes

These are used by the concurrent client implementation. A MPSC ring buffer with pre-allocated slots (disruptor-like) is
used as the concurrent client's entry point of outbound messages. The slots are populated by scratch instances.

```java
public final class AcquireRequestScratch implements AcquireRequestDecode, AcquireRequestEncode {
  // -- Output omitted for brevity -- //
}

```

## Usage

Having generated the classes, the only thing remaining to complete the service is to implement the server logic. The
following is taken from the [sample project](./sample/src/main/java/stoufexis/sample/LeaseServer.java).

```java
public class LeaseServer implements Agent, AutoCloseable {
  private final LeaseSingleThreadedJarpcServer singleThreadedJarpcServer;
  private final StateMachine stateMachine;

  public LeaseServer(Aeron aeron, ConnectionConfig cfg) {
    this.stateMachine = new StateMachine();
    this.singleThreadedJarpcServer =
        LeaseSingleThreadedJarpcServer.create(aeron, cfg, stateMachine);
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

  // The LeaseSingleThreadedStateMachine interface groups all functionality that needs to be
  // implemented by the user and passed to LeaseSingleThreadedJarpcServer.create
  private static final class StateMachine implements LeaseSingleThreadedStateMachine {

    @Override
    public boolean onRequest(
        long clientId,
        long correlationId,
        AcquireRequestDecode t,
        LeaseSingleThreadedServer server) {
      // -- Output omitted for brevity -- //
    }

    @Override
    public boolean onRequest(
        long clientId, long correlationId, QueryRequestDecode t, LeaseSingleThreadedServer server) {
      // -- Output omitted for brevity -- //
    }

    @Override
    public boolean onRequest(
        long clientId,
        long correlationId,
        RefreshRequestDecode t,
        LeaseSingleThreadedServer server) {
      // -- Output omitted for brevity -- //
    }

    @Override
    public void onClientDisconnected(long clientId) {
      // -- Output omitted for brevity -- //
    }

    int onTick() {
      // -- Output omitted for brevity -- //
    }

    @Override
    public void onProcessingError(
        long clientId, long correlationId, int messageType, RuntimeException error) {
      // -- Output omitted for brevity -- //
    }

    @Override
    public void onError(Throwable throwable) {
      // -- Output omitted for brevity -- //
    }
  }
}
```

Then, the server can be started, e.g.

```java
static void main() {
  try (MediaDriver mediaDriver = mediaDriver();
       Aeron aeron = aeron(mediaDriver);
       LeaseServer server = new LeaseServer(aeron, connectionConfig);
       AgentRunner agentRunner = runner(server)) {
    agentRunner.run();
  }
}
```

The client can be used without defining any additional logic. Note that Jarpc supports multiple clients targeting the
same server instance out-of-the-box, by
using [Response Channels](https://github.com/aeron-io/aeron/wiki/Response-Channels).

Note that the sample project code does not avoid all allocation, to keep it simple. It also demonstrates that, while
jarpc internals are explicitly 0-allocation, users do not need to adhere to the same standard to make good use of jarpc.

Usage of the single threaded client is not shown in the sample project, but
the [generated LeaseConcurrentJarpcClient](./sample/src/main/java/stoufexis/sample/generated/client/LeaseConcurrentJarpcClient.java)
code can be inspected instead, since it uses the single threaded client internally.

```java
void main() {
  try (MediaDriver mediaDriver = mediaDriver();
       Aeron aeron = aeron(mediaDriver);
       LeaseConcurrentJarpcClient client = LeaseConcurrentJarpcClient.create(aeron, connectionConfig, errorHandler, 1024);
       AgentRunner agentRunner = runner(client)) {

    AgentRunner.startOnThread(agentRunner);

    while (!client.isConnected()) Thread.sleep(100);

    // Use client instance. See the sample project for example usage of the lease client.
  }
}

```

## Code Generation Considerations

The generated code is meant to strike a balance between performance, size, readability, and testability. It should be
possible to read and interpret directly, but it sacrifices readability to reduce the generated size and move more code
to the library `stoufexis.jarpc.lib`, where it can be tested directly. Abstract classes and utility data structures are
heavily used for this purpose, even in ways that obscure the actual logic.

## Performance

Jarpc's single-threaded classes are a fairly thin wrapper around aeron Response Channels, that standardize message
encoding/decoding and request/response correlation. The concurrent client wraps the single threaded client and
introduces a ring buffer as the entry point for messages. This is the minimum overhead necessary to make the concurrent
client implementation possible.

Additionally, agrona datastructures are used across the generated code and there are no steady-state allocations
introduced.

The generated code avoids forcing a level of abstraction that turns many calls in the hotpath into megamorphic calls
when the number of RPC definitions grows large. As such, when it is simple to avoid megamorphic dispatch by repeating
code for each rpc type, without making the generated code completely un-readable or giant, jarpc repeats the code.

Real benchmarks are necessary to make any explicit claims about performance, but Jarpc is designed to perform very
close, if not identically to using plain Aeron transport with custom message serialization.

## Code Generation Internals

The code generation is built around a custom templating format. Each type of class/interface has a template file, which
defines placeholders and per-type or per-field loops. Each template is parsed by the generator CLI and populated using
the json specification of the service.

See an example template [here](./generator/src/main/resources/template/client/SingleThreadedJarpcClient.java)

## Data Model Limitations and SBE

The data model is fairly limited, with the most glaring omissions being composite types and variable length fields. In
the future, Jarpc's own decoding/encoding code can be replaced by SBE, which would unlock a much richer request/response
types.