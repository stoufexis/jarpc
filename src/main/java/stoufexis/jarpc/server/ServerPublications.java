package stoufexis.jarpc.server;

import io.aeron.Publication;
import org.agrona.CloseHelper;
import org.jctools.maps.NonBlockingHashMapLong;

public final class ServerPublications {

  // FIXME this is perhaps not the best data structure for this use-case.
  //  removes leave behind tombstones, which are not re-used since keys do not repeat,
  //  which forces a somewhat expensive periodic compaction.
  //  Consider replacing this with a purpose-built data structure instead.
  private final NonBlockingHashMapLong<Publication> clientToPublicationMap =
      new NonBlockingHashMapLong<>();

  public void put(long clientId, Publication pub) {
    clientToPublicationMap.put(clientId, pub);
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
