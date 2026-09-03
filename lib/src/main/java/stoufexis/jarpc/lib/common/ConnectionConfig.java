package stoufexis.jarpc.lib.common;

import io.aeron.Aeron;

public record ConnectionConfig(
    Aeron aeron,
    String requestEndpoint,
    int requestStreamId,
    String responseControl,
    int responseStreamId) {}
