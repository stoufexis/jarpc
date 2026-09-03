package stoufexis.jarpc.gen.model;

public record Replacement(String placeholder, String value) {
  public String applyTo(String base) {
    return base.replace(placeholder, value);
  }
}
