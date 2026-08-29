package stoufexis.jarpc.model;

public interface Encode {
  ErrorCode code();

  long correlationId();

  void commit();

  void abort();
}
