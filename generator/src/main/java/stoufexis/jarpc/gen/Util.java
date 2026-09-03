package stoufexis.jarpc.gen;

public class Util {
  private Util() {}

  public static String path(String... parts) {
    return "/" + String.join("/", parts);
  }

  public static String javaFile(String... parts) {
    return "/" + String.join("/", parts) + ".java";
  }
}
