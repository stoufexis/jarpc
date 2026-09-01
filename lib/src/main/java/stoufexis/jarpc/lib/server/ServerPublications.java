package stoufexis.jarpc.lib.server;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.CloseHelper;
import org.agrona.collections.Long2ObjectHashMap;

import static stoufexis.jarpc.lib.util.Util.createExclusiveServerPublication;

public final class ServerPublications {

  private final Long2ObjectHashMap<Publication> clientToPublicationMap = new Long2ObjectHashMap<>();

  private final String responseControl;
  private final Aeron aeron;
  private final int responseStreamId;

  public ServerPublications(String responseControl, Aeron aeron, int responseStreamId) {
    this.responseControl = responseControl;
    this.aeron = aeron;
    this.responseStreamId = responseStreamId;
  }

  public Publication ensurePublicationExists(long clientId) {
    // We don't need computeIfAbsent, put/remove only happen in the agent thread.
    Publication publication = clientToPublicationMap.get(clientId);
    if (null == publication) {
      publication =
          createExclusiveServerPublication(aeron, clientId, responseControl, responseStreamId);
      clientToPublicationMap.put(clientId, publication);
    }

    return publication;
  }

  public Publication remove(long clientId) {
    return clientToPublicationMap.remove(clientId);
  }

  public void closeAll() {
    clientToPublicationMap.values().forEach(CloseHelper::quietClose);
  }

  public Publication get(long clientId) {
    return clientToPublicationMap.get(clientId);
  }
}
