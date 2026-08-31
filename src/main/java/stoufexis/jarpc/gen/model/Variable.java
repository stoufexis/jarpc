package stoufexis.jarpc.gen.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public record Variable(String value) {

  public Variable {
    if (!value.matches("^[a-z][a-zA-Z0-9]*$")) {
      throw new IllegalArgumentException("expected " + value + " to be in camel case");
    }
  }

  private static String[] splitWords(String input) {
    return input.split("(?<!^)(?=[A-Z])");
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  String toCamelCase() {
    String[] words = splitWords(value);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      String w = words[i].toLowerCase();
      sb.append(i == 0 ? w : capitalize(w));
    }
    return sb.toString();
  }

  String toPascalCase() {
    return Arrays.stream(splitWords(value))
        .map(w -> capitalize(w.toLowerCase()))
        .collect(Collectors.joining());
  }

  String toSnakeCaseUpper() {
    return Arrays.stream(splitWords(value))
        .map(String::toUpperCase)
        .collect(Collectors.joining("_"));
  }
}
