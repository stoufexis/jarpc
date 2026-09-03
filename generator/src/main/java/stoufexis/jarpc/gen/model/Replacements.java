package stoufexis.jarpc.gen.model;

import java.util.List;

public record Replacements(List<Replacement> replacements) {

  public String applyTo(String base) {
    for (Replacement replacement : replacements) base = replacement.applyTo(base);
    return base;
  }

  public static Replacements of(Replacement... replacements) {
    return new Replacements(List.of(replacements));
  }
}
