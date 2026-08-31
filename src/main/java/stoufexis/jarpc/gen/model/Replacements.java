package stoufexis.jarpc.gen.model;

import java.util.Arrays;
import java.util.List;

public record Replacements(List<Replacement> replacements) {

  public String applyTo(String base) {
    for (Replacement replacement : replacements) {
      base = base.replace(replacement.placeholder(), replacement.value());
    }
    return base;
  }

  public static Replacements of(Replacement... replacements) {
    return new Replacements(Arrays.stream(replacements).toList());
  }
}
