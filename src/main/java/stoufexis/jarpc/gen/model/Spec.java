package stoufexis.jarpc.gen.model;

import java.util.List;

public record Spec(Variable serviceName, String pkg, String outputPath, List<RpcType> spec) {

  public String root(String... rootStr) {
    return replacements().applyTo(String.join("\n", rootStr));
  }

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("%package%", pkg),
        new Replacement("%Service%", serviceName.toPascalCase()),
        new Replacement("%service%", serviceName.toCamelCase()),
        new Replacement("%SERVICE%", serviceName.toSnakeCaseUpper()));
  }
}
