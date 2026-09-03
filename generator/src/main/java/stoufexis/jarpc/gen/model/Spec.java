package stoufexis.jarpc.gen.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public record Spec(Variable serviceName, String pkg, List<RpcType> types) {

  public Replacements replacements() {
    return Replacements.of(
        new Replacement("_package_", pkg),
        new Replacement("_Service_", serviceName.toPascalCase()),
        new Replacement("_service_", serviceName.toCamelCase()),
        new Replacement("_SERVICE_", serviceName.toSnakeCaseUpper()));
  }

  public static Spec parse(String serviceName, String serviceSpec, String pkg) {
    int typeId = 1;
    LinkedList<RpcType> types = new LinkedList<>();
    HashSet<String> names = new HashSet<>();

    for (var obj : new JSONArray(serviceSpec)) {
      JSONObject rpc = (JSONObject) obj;
      Variable rpcName = var(rpc.getString("rpcName"));

      if (names.contains(rpcName.value().toLowerCase()))
        throw new IllegalArgumentException("Duplicate rpc names");

      List<Field> requestFields = getFields(rpc, "request");
      List<Field> responseFields = getFields(rpc, "response");

      names.add(rpcName.value().toLowerCase());
      types.add(
          new RpcType(
              rpcName,
              requestFields,
              responseFields,
              messageSize(requestFields),
              messageSize(responseFields),
              typeId++));
    }

    return new Spec(var(serviceName), pkg, List.copyOf(types));
  }

  private static int messageSize(List<Field> fields) {
    int size = 0;
    for (Field f : fields) size += f.fieldType().getLength();
    return size;
  }

  private static List<Field> getFields(JSONObject rpc, String key) {
    LinkedList<Field> fields = new LinkedList<>();
    int offset = 0;

    for (var obj2 : rpc.getJSONArray(key)) {
      JSONObject field = (JSONObject) obj2;
      String fieldName = field.getString("name");
      Type fieldType = Type.parse(field.getString("type"));
      fields.add(new Field(var(fieldName), fieldType, offset));
      offset += fieldType.getLength();
    }

    return List.copyOf(fields);
  }

  private static Variable var(String v) {
    return new Variable(v);
  }
}
