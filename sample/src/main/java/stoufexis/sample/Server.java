//package stoufexis.sample;
//
//import stoufexis.sample.generated.execution.common.CancelOrderRequestDecode;
//import stoufexis.sample.generated.execution.common.PostLimitOrderRequestDecode;
//import stoufexis.sample.generated.execution.server.ExecutionConcurrentServer;
//import stoufexis.sample.generated.updates.common.SubscribeOrderUpdatesRequestDecode;
//import stoufexis.sample.generated.updates.server.UpdatesConcurrentServer;
//
//public final class Server implements ExecutionConcurrentServer, UpdatesConcurrentServer {
//
//  private PostLimitOrderResponseHandler postLimitOrderResponseHandler;
//  private CancelOrderResponseHandler cancelOrderResponseHandler;
//  private SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesResponseHandler;
//
//  @Override
//  public void registerPostLimitOrder(PostLimitOrderResponseHandler postLimitOrderResponse) {
//    this.postLimitOrderResponseHandler = postLimitOrderResponse;
//  }
//
//  @Override
//  public void registerCancelOrder(CancelOrderResponseHandler cancelOrderResponse) {
//    this.cancelOrderResponseHandler = cancelOrderResponse;
//  }
//
//  @Override
//  public void registerSubscribeOrderUpdates(
//      SubscribeOrderUpdatesResponseHandler subscribeOrderUpdatesResponse) {
//    this.subscribeOrderUpdatesResponseHandler = subscribeOrderUpdatesResponse;
//  }
//
//  @Override
//  public boolean postLimitOrder(
//      long clientId, long correlationId, PostLimitOrderRequestDecode request) {
//    return false;
//  }
//
//  @Override
//  public boolean cancelOrder(long clientId, long correlationId, CancelOrderRequestDecode request) {
//    return false;
//  }
//
//  @Override
//  public boolean subscribeOrderUpdates(
//      long clientId, long correlationId, SubscribeOrderUpdatesRequestDecode request) {
//    return false;
//  }
//}
