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
    return foreachField(gen, requestFields);
  }

  public String foreachResponseField(String gen) {
    return foreachField(gen, responseFields);
  }

  private static String foreachField(String gen, List<Field> fields) {
    StringBuilder output = new StringBuilder();

    for (Field typ : fields) {
      output.append(typ.replacements().applyTo(gen));
      output.append("\n");
    }

    return output.toString();
  }

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("_type_", rpcName.toCamelCase()),
        new Replacement("_Type_", rpcName.toPascalCase()),
        new Replacement("_TYPE_", rpcName.toSnakeCaseUpper()),
        new Replacement("_typeRequestSize_", String.valueOf(requestSize)),
        new Replacement("_typeResponseSize_", String.valueOf(responseSize)),
        new Replacement("_typeId_", String.valueOf(typeId)));
  }
}
