package stoufexis.jarpc.gen.model;

public record Field(Variable fieldName, Variable fieldType, int fieldOffset) {
  public Replacements replacements() {
    return Replacements.of(
        new Replacement("_field_", fieldName.toCamelCase()),
        new Replacement("_Field_", fieldName.toPascalCase()),
        new Replacement("_FIELD_", fieldName.toSnakeCaseUpper()),
        new Replacement("_fieldType_", fieldType.toCamelCase()),
        new Replacement("_FieldType_", fieldType.toPascalCase()),
        new Replacement("_FIELD_TYPE_", fieldType.toSnakeCaseUpper()),
        new Replacement("_fieldOffset_", String.valueOf(fieldOffset)));
  }
}
