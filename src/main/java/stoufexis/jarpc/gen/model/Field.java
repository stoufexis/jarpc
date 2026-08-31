package stoufexis.jarpc.gen.model;

public record Field(Variable fieldName, Variable fieldType, int fieldOffset) {
  public Replacements replacements() {
    return Replacements.of(
        new Replacement("%field%", fieldName.toCamelCase()),
        new Replacement("%Field%", fieldName.toPascalCase()),
        new Replacement("%FIELD%", fieldName.toSnakeCaseUpper()),
        new Replacement("%fieldType%", fieldType.toCamelCase()),
        new Replacement("%FieldType%", fieldType.toPascalCase()),
        new Replacement("%FIELD_TYPE%", fieldType.toSnakeCaseUpper()),
        new Replacement("%fieldOffset%", String.valueOf(fieldOffset)));
  }
}
