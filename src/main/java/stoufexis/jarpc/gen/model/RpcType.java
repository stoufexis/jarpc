package stoufexis.jarpc.gen.model;

import java.util.List;

public record RpcType(
    Variable rpcName,
    List<Field> requestFields,
    List<Field> responseFields,
    int requestSize,
    int responseSize,
    int typeId) {

  public String foreachRequestField(String gen) {
    StringBuilder output = new StringBuilder();

    for (Field typ : requestFields) {
      output.append(typ.replacements().applyTo(gen));
      output.append("\n");
    }

    return output.toString();
  }

  public String foreachResponseField(String gen) {
    StringBuilder output = new StringBuilder();

    for (Field typ : responseFields) {
      output.append(typ.replacements().applyTo(gen));
      output.append("\n");
    }

    return output.toString();
  }

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("%type%", rpcName.toCamelCase()),
        new Replacement("%Type%", rpcName.toPascalCase()),
        new Replacement("%TYPE%", rpcName.toSnakeCaseUpper()));
  }
}
