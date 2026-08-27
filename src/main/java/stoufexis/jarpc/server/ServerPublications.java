package stoufexis.jarpc.server;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.CloseHelper;
import org.jctools.maps.NonBlockingHashMapLong;

import static stoufexis.jarpc.util.Util.createServerPublication;

public final class ServerPublications {

  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.
  private final NonBlockingHashMapLong<Publication> clientToPublicationMap =
      new NonBlockingHashMapLong<>();

  private final String responseControl;
  private final Aeron aeron;
  private final int responseStreamId;

  ServerPublications(String responseControl, Aeron aeron, int responseStreamId) {
    this.responseControl = responseControl;
    this.aeron = aeron;
    this.responseStreamId = responseStreamId;
  }

  Publication ensurePublicationExists(long clientId) {
    // We don't need computeIfAbsent, put/remove only happen in the agent thread.
    Publication publication = clientToPublicationMap.get(clientId);
    if (null == publication) {
      publication = createServerPublication(aeron, clientId, responseControl, responseStreamId);
      clientToPublicationMap.put(clientId, publication);
    }

    return publication;
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
