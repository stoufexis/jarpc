package stoufexis.jarpc.exchange.model;

import stoufexis.jarpc.model.Metadata;

public final class Catalog {
  public static final Metadata postOrder = new Metadata(1, 32, 4);

  public static final Metadata cancelAll = new Metadata(2, 0, 4);
}
