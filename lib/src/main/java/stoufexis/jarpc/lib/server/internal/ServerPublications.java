package stoufexis.jarpc.lib.server.internal;

import static stoufexis.jarpc.lib.common.Util.createExclusiveServerPublication;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.CloseHelper;
import org.agrona.collections.Long2ObjectHashMap;
import stoufexis.jarpc.lib.common.ConnectionConfig;

public final class ServerPublications {

  private final Long2ObjectHashMap<Publication> map = new Long2ObjectHashMap<>();

  private final Aeron aeron;
  private final ConnectionConfig connectionConfig;

  public ServerPublications(Aeron aeron, ConnectionConfig connectionConfig) {
    this.aeron = aeron;
    this.connectionConfig = connectionConfig;
  }

  public Publication ensurePublicationExists(long clientId) {
    Publication publication = map.get(clientId);
    if (null == publication) {
      publication = createExclusiveServerPublication(aeron, clientId, connectionConfig);
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
