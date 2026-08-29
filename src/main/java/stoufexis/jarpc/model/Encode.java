package stoufexis.jarpc.model;

public interface Encode {
  /** Either the correlation id assigned (positive), or an ErrorCode result (negative) */
  int claimResult();

  void commit();

  void abort();
}
