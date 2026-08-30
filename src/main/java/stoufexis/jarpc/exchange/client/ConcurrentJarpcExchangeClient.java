package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import stoufexis.jarpc.client.ClientErrorHandler;
import stoufexis.jarpc.exchange.model.*;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.MPSCRingBuffer;

// FIXME add timeouts and ad-hoc cancel

public class ConcurrentJarpcExchangeClient implements ConcurrentExchangeClient, Agent {

  private final SingleThreadedJarpcExchangeClient singleThreadedClient;

  private final MPSCRingBuffer<PostOrderRequestScratch> postOrderRequests;
  private final MPSCRingBuffer<CancelAllRequestScratch> cancelAllRequests;

  private final ClientErrorHandler errorHandler;

  ConcurrentJarpcExchangeClient(
      Publication publication,
      Subscription subscription,
      ClientErrorHandler errorHandler,
      int queueCapacity) {
    this.singleThreadedClient =
        new SingleThreadedJarpcExchangeClient(
            publication,
            subscription,
            postOrderResponseHandler,
            cancelAllResponseHandler,
            errorHandler);
    this.postOrderRequests = new MPSCRingBuffer<>(queueCapacity, PostOrderRequestScratch::new);
    this.cancelAllRequests = new MPSCRingBuffer<>(queueCapacity, CancelAllRequestScratch::new);
    this.errorHandler = errorHandler;
  }

  @Override
  public boolean postOrder(PostOrderRequestDecode request, PostOrderResponseHandler response) {
    return postOrderRequests.offer(PostOrderRequestScratch::setter, request, response);
  }

  @Override
  public boolean cancelAll(CancelAllRequestDecode request, CancelAllResponseHandler response) {
    return cancelAllRequests.offer(CancelAllRequestScratch::setter, request, response);
  }

  private final Long2ObjectHashMap<PostOrderResponseHandler> postOrderCallbacks =
      new Long2ObjectHashMap<>();

  private final Long2ObjectHashMap<CancelAllResponseHandler> cancelAllCallbacks =
      new Long2ObjectHashMap<>();

  private final PostOrderResponseHandler postOrderResponseHandler =
      new PostOrderResponseHandler() {
        private PostOrderResponseHandler getCallback(long correlationId) {
          PostOrderResponseHandler callback = postOrderCallbacks.get(correlationId);

          if (callback == null) {
            errorHandler.onCallbackNotFound(correlationId, "PostOrder");
          }

          return callback;
        }

        @Override
        public boolean onResponse(long correlationId, PostOrderResponseDecode t) {
          PostOrderResponseHandler callback = getCallback(correlationId);

          boolean dispatched = false;

          if (callback != null) {
            dispatched = callback.onResponse(correlationId, t);
          }

          if (dispatched) {
            postOrderCallbacks.remove(correlationId);
          }

          return true;
        }

        @Override
        public boolean onClientDecodeError(long correlationId) {
          PostOrderResponseHandler callback = getCallback(correlationId);

          boolean dispatched = false;

          if (callback != null) {
            dispatched = callback.onClientDecodeError(correlationId);
          }

          if (dispatched) {
            postOrderCallbacks.remove(correlationId);
          }

          return true;
        }

        @Override
        public boolean onServerDecodeError(long correlationId) {
          PostOrderResponseHandler callback = getCallback(correlationId);

          boolean dispatched = false;

          if (callback != null) {
            dispatched = callback.onServerDecodeError(correlationId);
          }

          if (dispatched) {
            postOrderCallbacks.remove(correlationId);
          }

          return true;
        }
      };

  private final CancelAllResponseHandler cancelAllResponseHandler =
      new CancelAllResponseHandler() {
        private CancelAllResponseHandler getCallback(long correlationId) {
          CancelAllResponseHandler callback = cancelAllCallbacks.get(correlationId);

          if (callback == null) {
            errorHandler.onCallbackNotFound(correlationId, "PostOrder");
          }

          return callback;
        }

        @Override
        public boolean onResponse(long correlationId, CancelAllResponseDecode t) {
          CancelAllResponseHandler callback = getCallback(correlationId);

          boolean dispatched = false;

          if (callback != null) {
            dispatched = callback.onResponse(correlationId, t);
          }

          if (dispatched) {
            cancelAllCallbacks.remove(correlationId);
          }

          return true;
        }

        @Override
        public boolean onClientDecodeError(long correlationId) {
          CancelAllResponseHandler callback = getCallback(correlationId);

          boolean dispatched = false;

          if (callback != null) {
            dispatched = callback.onClientDecodeError(correlationId);
          }

          if (dispatched) {
            cancelAllCallbacks.remove(correlationId);
          }

          return true;
        }

        @Override
        public boolean onServerDecodeError(long correlationId) {
          CancelAllResponseHandler callback = getCallback(correlationId);

          boolean dispatched = false;

          if (callback != null) {
            dispatched = callback.onServerDecodeError(correlationId);
          }

          if (dispatched) {
            cancelAllCallbacks.remove(correlationId);
          }

          return true;
        }
      };

  private boolean postOrderPopulated = false;
  private final PostOrderRequestScratch postOrder = new PostOrderRequestScratch();

  private boolean cancelAllPopulated = false;
  private final CancelAllRequestScratch cancelAll = new CancelAllRequestScratch();

  @Override
  public int doWork() {
    int work = 0;

    if (!postOrderPopulated) {
      postOrderPopulated = postOrderRequests.poll(PostOrderRequestScratch::copy, postOrder);
    }

    if (postOrderPopulated) {
      PostOrderRequestEncode encode = singleThreadedClient.claimPostOrder();

      if (encode.code() == ErrorCode.BACKPRESSURE) {
        return work;
      } else if (encode.code() != null) {
        errorHandler.onCorruptPublication(encode.code());
        postOrderPopulated = false;
      } else {
        postOrderCallbacks.put(encode.correlationId(), postOrder.getHandler());
        encode.set(postOrder);
        postOrderPopulated = false;
        work++;
      }
    }

    if (!cancelAllPopulated) {
      cancelAllPopulated = cancelAllRequests.poll(CancelAllRequestScratch::copy, cancelAll);
    }

    if (cancelAllPopulated) {
      CancelAllRequestEncode encode = singleThreadedClient.claimCancelAll();

      if (encode.code() == ErrorCode.BACKPRESSURE) {
        return work;
      } else if (encode.code() != null) {
        errorHandler.onCorruptPublication(encode.code());
        cancelAllPopulated = false;
      } else {
        cancelAllCallbacks.put(encode.correlationId(), cancelAll.getHandler());
        encode.set(cancelAll);
        cancelAllPopulated = false;
        work++;
      }
    }

    work += singleThreadedClient.poll(1);

    return work;
  }

  @Override
  public String roleName() {
    return "FuturesJarpcExchangeClient";
  }
}
