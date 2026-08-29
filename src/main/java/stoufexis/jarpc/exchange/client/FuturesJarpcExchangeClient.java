package stoufexis.jarpc.exchange.client;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import java.util.concurrent.CompletableFuture;
import stoufexis.jarpc.exchange.client.SingleThreadedExchangeClient.*;
import stoufexis.jarpc.model.ErrorCode;
import stoufexis.jarpc.util.PublicationError;

public class FuturesJarpcExchangeClient implements FuturesExchangeClient, Agent {
  private final SingleThreadedJarpcExchangeClient singleThreadedClient;

  private final ManyToOneConcurrentArrayQueue<RequestPair<PostOrderRequest, PostOrderResponse>>
      postOrderRequests;

  private final ManyToOneConcurrentArrayQueue<RequestPair<CancelAllRequest, CancelAllResponse>>
      cancelAllRequests;

  private final Long2ObjectHashMap<CompletableFuture<PostOrderResponse>> postOrderCallbacks;
  private final Long2ObjectHashMap<CompletableFuture<CancelAllResponse>> cancelAllCallbacks;

  FuturesJarpcExchangeClient(
      SingleThreadedJarpcExchangeClient singleThreadedClient, int queueCapacity) {
    this.singleThreadedClient = singleThreadedClient;
    this.postOrderRequests = new ManyToOneConcurrentArrayQueue<>(queueCapacity);
    this.cancelAllRequests = new ManyToOneConcurrentArrayQueue<>(queueCapacity);
    this.postOrderCallbacks = new Long2ObjectHashMap<>();
    this.cancelAllCallbacks = new Long2ObjectHashMap<>();
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

  private RequestPair<PostOrderRequest, PostOrderResponse> postOrder = null;
  private RequestPair<CancelAllRequest, CancelAllResponse> cancelAll = null;

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
        cancelAll = null;
      } else {
        cancelAllCallbacks.put(encode.correlationId(), cancelAll.response);
        encode.commit();
        work++;
        cancelAll = null;
      }
    }

    return work;
  }

  @Override
  public String roleName() {
    return "FuturesJarpcExchangeClient";
  }

  private record RequestPair<Req, Res>(Req request, CompletableFuture<Res> response) {}
}
