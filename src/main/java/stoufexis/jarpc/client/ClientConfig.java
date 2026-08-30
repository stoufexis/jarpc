package stoufexis.jarpc.client;

import io.aeron.Aeron;

public record ClientConfig(
    Aeron aeron,
    String requestEndpoint,
    int requestStreamId,
    String responseControl,
    int responseStreamId) {}
