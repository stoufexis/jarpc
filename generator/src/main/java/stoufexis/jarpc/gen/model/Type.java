package stoufexis.jarpc.gen.model;

public abstract class Type {
  private final int length;
  private final Variable name;
  private final String javaType;

  protected Type(int length, String name, String javaType) {
    this.length = length;
    this.name = new Variable(name);
    this.javaType = javaType;
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

  static final class Byte extends Type {
    Byte() {
      super(1, "byte", "byte");
    }
  }

  static final class Short extends Type {
    Short() {
      super(2, "short", "short");
    }
  }

  static final class Int extends Type {
    Int() {
      super(4, "int", "int");
    }
  }

  static final class Long extends Type {
    Long() {
      super(8, "long", "long");
    }
  }

  static final class Float extends Type {
    Float() {
      super(4, "float", "float");
    }
  }

  static final class Double extends Type {
    Double() {
      super(8, "double", "double");
    }
  }

  public static final class Bytes extends Type {
    Bytes(int length) {
      super(length, "bytes", "bytes");
    }
  }
}
