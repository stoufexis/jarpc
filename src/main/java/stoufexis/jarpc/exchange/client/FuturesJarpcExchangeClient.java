package stoufexis.jarpc.exchange.client;

import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.ErrorHandler;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import java.util.concurrent.CompletableFuture;

import stoufexis.jarpc.client.ClientErrorHandler;
import stoufexis.jarpc.exchange.client.SingleThreadedExchangeClient.*;
import stoufexis.jarpc.exchange.server.ExchangeServer;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.PublicationError;

public class FuturesJarpcExchangeClient implements FuturesExchangeClient, Agent {
  private final SingleThreadedJarpcExchangeClient singleThreadedClient;

  private final ManyToOneConcurrentArrayQueue<RequestPair<PostOrderRequest, PostOrderResponse>>
      postOrderRequests;

  private final ManyToOneConcurrentArrayQueue<RequestPair<CancelAllRequest, CancelAllResponse>>
      cancelAllRequests;

  private final ClientErrorHandler errorHandler;

  FuturesJarpcExchangeClient(
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
    this.postOrderRequests = new ManyToOneConcurrentArrayQueue<>(queueCapacity);
    this.cancelAllRequests = new ManyToOneConcurrentArrayQueue<>(queueCapacity);
    this.errorHandler = errorHandler;
  }

  @Override
  public boolean postOrder(
      PostOrderRequest request, CompletableFuture<PostOrderResponse> response) {
    return postOrderRequests.offer(new RequestPair<>(request, response));
  }

  @Override
  public boolean cancelAll(
      CancelAllRequest request, CompletableFuture<CancelAllResponse> response) {
    return cancelAllRequests.offer(new RequestPair<>(request, response));
  }

  private final Long2ObjectHashMap<CompletableFuture<PostOrderResponse>> postOrderCallbacks =
      new Long2ObjectHashMap<>();

  private final Long2ObjectHashMap<CompletableFuture<CancelAllResponse>> cancelAllCallbacks =
      new Long2ObjectHashMap<>();

  private RequestPair<PostOrderRequest, PostOrderResponse> postOrder = null;
  private RequestPair<CancelAllRequest, CancelAllResponse> cancelAll = null;

  private final PostOrderResponseHandler postOrderResponseHandler =
      new PostOrderResponseHandler() {
        @Override
        public boolean onResponse(long correlationId, PostOrderResponseDecode t) {
          CompletableFuture<PostOrderResponse> callback = postOrderCallbacks.get(correlationId);
          if (callback == null) {
            errorHandler.onCallbackNotFound(correlationId, "PostOrder");
          } else {
            callback.complete(new PostOrderResponse(t.getStatusCode()));
          }

          return true;
        }

        @Override
        public boolean onClientDecodeError(long correlationId) {
          return false;
        }

        @Override
        public boolean onServerDecodeError(long correlationId) {
          return false;
        }
      };

  private final CancelAllResponseHandler cancelAllResponseHandler =
      new CancelAllResponseHandler() {
        @Override
        public boolean onResponse(long correlationId, CancelAllResponseDecode t) {
          return false;
        }

        @Override
        public boolean onClientDecodeError(long correlationId) {
          return false;
        }

        @Override
        public boolean onServerDecodeError(long correlationId) {
          return false;
        }
      };

  @Override
  public int doWork() {
    int work = 0;

    if (postOrder == null) {
      postOrder = postOrderRequests.poll();
    }

    if (postOrder != null) {
      PostOrderRequestEncode encode = singleThreadedClient.claimPostOrder();

      if (encode.code() == ErrorCode.BACKPRESSURE) {
        return work;
      } else if (encode.code() != null) {
        postOrder.response.completeExceptionally(new PublicationError(encode.code()));
        postOrder = null;
        work++;
      } else {
        postOrderCallbacks.put(encode.correlationId(), postOrder.response);
        encode.setBaseAssetId(postOrder.request.baseAssetId());
        encode.setQuoteAssetId(postOrder.request.quoteAssetId());
        encode.setQuantityUnscaled(postOrder.request.quantityUnscaled());
        encode.setQuantityScale(postOrder.request.quantityScale());
        encode.setRateUnscaled(postOrder.request.rateUnscaled());
        encode.setRateScale(postOrder.request.rateScale());
        encode.commit();
        work++;
        postOrder = null;
      }
    }

    if (cancelAll == null) {
      cancelAll = cancelAllRequests.poll();
    }

    if (cancelAll != null) {
      CancelAllRequestEncode encode = singleThreadedClient.claimCancelAll();

      if (encode.code() == ErrorCode.BACKPRESSURE) {
        return work;
      } else if (encode.code() != null) {
        cancelAll.response.completeExceptionally(new PublicationError(encode.code()));
        work++;
        cancelAll = null;
      } else {
        cancelAllCallbacks.put(encode.correlationId(), cancelAll.response);
        encode.commit();
        work++;
        cancelAll = null;
      }
    }

    work += singleThreadedClient.poll(1);

    return work;
  }

  @Override
  public String roleName() {
    return "FuturesJarpcExchangeClient";
  }

  private record RequestPair<Req, Res>(Req request, CompletableFuture<Res> response) {}
}
