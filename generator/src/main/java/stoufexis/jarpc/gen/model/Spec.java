package stoufexis.jarpc.gen.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public record Spec(Variable serviceName, String pkg, List<RpcType> types) {

  public static Spec parse(String serviceName, String serviceSpec, String pkg) {
    Variable service = var(serviceName);

    LinkedList<RpcType> types = new LinkedList<>();
    JSONArray specJson = new JSONArray(serviceSpec);

    int typeId = 1;
    for (var obj : specJson) {
      JSONObject rpc = (JSONObject) obj;

      Variable rpcName = var(rpc.getString("rpcName"));
      List<Field> requestFields = getFields(rpc, "request");
      List<Field> responseFields = getFields(rpc, "response");
      types.add(
          new RpcType(
              rpcName,
              requestFields,
              responseFields,
              messageSize(requestFields),
              messageSize(responseFields),
              typeId++));
    }

    return new Spec(service, pkg, List.copyOf(types));
  }

  private static int messageSize(List<Field> fields) {
    int size = 0;
    for (var field : fields) {
      size += field.fieldType().getLength();
    }

    return size;
  }

  private static List<Field> getFields(JSONObject rpc, String key) {
    LinkedList<Field> fields = new LinkedList<>();
    int offset = 0;

    for (var obj2 : rpc.getJSONArray(key)) {
      JSONObject field = (JSONObject) obj2;
      String fieldName = field.getString("name");
      Type fieldType = parseType(field.getString("type"));
      fields.add(new Field(var(fieldName), fieldType, offset));
      offset += fieldType.getLength();
    }

    return List.copyOf(fields);
  }

  private static Type parseType(String type) {
    return switch (type.toLowerCase()) {
      case "byte" -> new Type.Byte();
      case "short" -> new Type.Short();
      case "int" -> new Type.Int();
      case "float" -> new Type.Float();
      case "long" -> new Type.Long();
      case "double" -> new Type.Double();
      case String w when w.startsWith("bytes") ->
          new Type.Bytes(Integer.parseInt(w.replace("bytes", "")));
      default -> throw new IllegalArgumentException("Unsupported type: " + type);
    };
  }

  private static Variable var(String v) {
    return new Variable(v);
  }

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("_package_", pkg),
        new Replacement("_Service_", serviceName.toPascalCase()),
        new Replacement("_service_", serviceName.toCamelCase()),
        new Replacement("_SERVICE_", serviceName.toSnakeCaseUpper()));
  }
}
