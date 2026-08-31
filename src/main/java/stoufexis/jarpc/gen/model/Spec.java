package stoufexis.jarpc.gen.model;

import java.util.List;

public record Spec(Variable serviceName, String pkg, String outputPath, List<RpcType> spec) {

  public String root(String... rootStr) {
    return replacements().applyTo(String.join("\n", rootStr));
  }

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("_package_", pkg),
        new Replacement("_Service_", serviceName.toPascalCase()),
        new Replacement("_service_", serviceName.toCamelCase()),
        new Replacement("_SERVICE_", serviceName.toSnakeCaseUpper()));
  }
}
