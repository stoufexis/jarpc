package stoufexis.jarpc.server;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.Image;
import io.aeron.Publication;
import org.agrona.CloseHelper;
import org.jctools.maps.NonBlockingHashMapLong;

final class ServerPublications {

  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.
  private final NonBlockingHashMapLong<Publication> clientToPublicationMap =
      new NonBlockingHashMapLong<>();

  private final ChannelUriStringBuilder responseUriBuilder;
  private final Aeron aeron;
  private final int responseStreamId;

  ServerPublications(String responseControl, Aeron aeron, int responseStreamId) {
    this.responseUriBuilder =
        new ChannelUriStringBuilder()
            .media("udp")
            .controlMode("response")
            .controlEndpoint(responseControl);
    this.aeron = aeron;
    this.responseStreamId = responseStreamId;
  }

  void ensurePublicationExists(Image image) {
    // We don't need computeIfAbsent, put/remove only happen in the agent thread.
    if (null == get(image.correlationId())) {
      Publication publication =
          aeron.addPublication(
              responseUriBuilder.responseCorrelationId(image.correlationId()).build(),
              responseStreamId);

      put(image.correlationId(), publication);
    }
  }

  void put(long clientId, Publication pub) {
    clientToPublicationMap.put(clientId, pub);
  }

  Publication remove(long clientId) {
    return clientToPublicationMap.remove(clientId);
  }

  void closeAll() {
    clientToPublicationMap.values().forEach(CloseHelper::quietClose);
  }

  Publication get(long clientId) {
    return clientToPublicationMap.get(clientId);
  }
}
