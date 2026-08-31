package stoufexis.jarpc.model;

import io.aeron.Aeron;

public record ConnectivityConfig(
    Aeron aeron,
    String requestEndpoint,
    int requestStreamId,
    String responseControl,
    int responseStreamId) {}
