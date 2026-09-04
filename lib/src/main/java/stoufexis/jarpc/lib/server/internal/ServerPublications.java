package stoufexis.jarpc.lib.server.internal;

import static stoufexis.jarpc.lib.common.Util.createExclusiveServerPublication;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.CloseHelper;
import org.agrona.collections.Long2ObjectHashMap;

public final class ServerPublications {

  private final Long2ObjectHashMap<Publication> map = new Long2ObjectHashMap<>();

  private final String responseControl;
  private final Aeron aeron;
  private final int responseStreamId;

  public ServerPublications(String responseControl, Aeron aeron, int responseStreamId) {
    this.responseControl = responseControl;
    this.aeron = aeron;
    this.responseStreamId = responseStreamId;
  }

  public Publication ensurePublicationExists(long clientId) {
    Publication publication = map.get(clientId);
    if (null == publication) {
      publication =
          createExclusiveServerPublication(aeron, clientId, responseControl, responseStreamId);
      map.put(clientId, publication);
    }
    return publication;
  }

  public Publication remove(long clientId) {
    return map.remove(clientId);
  }

  public void closeAll() {
    map.values().forEach(CloseHelper::quietClose);
  }

  public Publication get(long clientId) {
    return map.get(clientId);
  }
}
