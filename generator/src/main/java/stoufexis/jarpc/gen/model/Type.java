package stoufexis.jarpc.gen.model;

public enum Type {
  BOOLEAN(1, "boolean", "boolean", "false", false),
  BYTE(1, "byte", "byte", "0", false),
  SHORT(2, "short", "short", "0", false),
  INT(4, "int", "int", "0", false),
  LONG(8, "long", "long", "0", false),
  FLOAT(4, "float", "float", "0", false),
  DOUBLE(8, "double", "double", "0", false),
  BYTES16(16, "bytes16", "Bytes", "new Bytes(16)", true),
  BYTES32(32, "bytes32", "Bytes", "new Bytes(32)", true),
  BYTES64(64, "bytes64", "Bytes", "new Bytes(64)", true),
  BYTES128(128, "bytes128", "Bytes", "new Bytes(128)", true);

  private final int length;
  private final Variable name;
  private final String javaType;
  private final String initialValue;
  private final boolean isArray;

  Type(int length, String name, String javaType, String initialValue, boolean isArray) {
    this.length = length;
    this.name = new Variable(name);
    this.javaType = javaType;
    this.initialValue = initialValue;
    this.isArray = isArray;
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

  public boolean isArray() {
    return isArray;
  }

  public String getInitialValue() {
    return initialValue;
  }
}
