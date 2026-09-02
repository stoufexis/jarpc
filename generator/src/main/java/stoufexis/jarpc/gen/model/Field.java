package stoufexis.jarpc.gen.model;

public record Field(Variable fieldName, Type fieldType, int fieldOffset) {

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("_field_", fieldName.toCamelCase()),
        new Replacement("_Field_", fieldName.toPascalCase()),
        new Replacement("_FIELD_", fieldName.toSnakeCaseUpper()),
        new Replacement("_getFieldParam_", fieldType.getFieldParam()),
        new Replacement("_getFieldParamTarget_", fieldType.getFieldParamTarget()),
        new Replacement("_javaType_", fieldType.getJavaType()),
        new Replacement("_fieldType_", fieldType.getName().toCamelCase()),
        new Replacement("_FieldType_", fieldType.getName().toPascalCase()),
        new Replacement("_FIELD_TYPE_", fieldType.getName().toSnakeCaseUpper()),
        new Replacement("_fieldOffset_", String.valueOf(fieldOffset)));
  }
}
