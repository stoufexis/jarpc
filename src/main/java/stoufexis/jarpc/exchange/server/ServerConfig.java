package stoufexis.jarpc.exchange.server;

import io.aeron.Aeron;

public record ServerConfig(
    Aeron aeron,
    String requestEndpoint,
    int requestStreamId,
    String responseControl,
    int responseStreamId) {}
