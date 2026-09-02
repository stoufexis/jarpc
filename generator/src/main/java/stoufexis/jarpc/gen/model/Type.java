package stoufexis.jarpc.gen.model;

public enum Type {
  BOOLEAN(1, "boolean", "boolean", "", ""),
  BYTE(1, "byte", "byte", "", ""),
  SHORT(2, "short", "short", "", ""),
  INT(4, "int", "int", "", ""),
  LONG(8, "long", "long", "", ""),
  FLOAT(4, "float", "float", "", ""),
  DOUBLE(8, "double", "double", "", ""),
  BYTES16(16, "bytes16", "byte[]", "byte[] src", ", src"),
  BYTES32(32, "bytes32", "byte[]", "byte[] src", ", src"),
  BYTES64(64, "bytes64", "byte[]", "byte[] src", ", src"),
  BYTES128(128, "bytes128", "byte[]", "byte[] src", ", src");

  private final int length;
  private final Variable name;
  private final String javaType;
  private final String fieldParam;
  private final String fieldParamTarget;

  Type(int length, String name, String javaType, String fieldParam, String fieldParamTarget) {
    this.length = length;
    this.name = new Variable(name);
    this.javaType = javaType;
    this.fieldParam = fieldParam;
    this.fieldParamTarget = fieldParamTarget;
  }

  public String getJavaType() {
    return javaType;
  }

  public Variable getName() {
    return name;
  }

  public int getLength() {
    return length;
  }

  public String getFieldParamTarget() {
    return fieldParamTarget;
  }

  public String getFieldParam() {
    return fieldParam;
  }
}
