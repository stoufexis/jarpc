package stoufexis.jarpc.gen;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Generator {
  private Variable pkg;
  private Variable service;
  private List<RpcType> rpcTypes;

  public void parseSpec(String serviceName, String serviceSpec, List<String> packageComponents) {
    this.pkg = var(String.join(".", packageComponents));
    this.service = var(serviceName);

    LinkedList<RpcType> types = new LinkedList<>();
    JSONArray specJson = new JSONArray(serviceSpec);

    for (var obj : specJson) {
      JSONObject rpc = (JSONObject) obj;

      Variable rpcName = var(rpc.getString("rpcName"));
      List<Field> requestFields = getFields(rpc, "request");
      List<Field> responseFields = getFields(rpc, "response");
      types.add(new RpcType(rpcName, requestFields, responseFields));
    }

    rpcTypes = List.copyOf(types);
  }

  private static List<Field> getFields(JSONObject rpc, String key) {
    LinkedList<Field> fields = new LinkedList<>();
    int offset = 0;

    for (var obj2 : rpc.getJSONArray(key)) {
      JSONObject field = (JSONObject) obj2;
      String fieldName = field.getString("name");
      String fieldType = field.getString("type");
      fields.add(new Field(var(fieldName), var(fieldType), offset));
      offset += typeLength(fieldType);
    }

    return List.copyOf(fields);
  }

  private static int typeLength(String type) {
    return switch (type.toLowerCase()) {
      case "int" -> 4;
      case "long" -> 8;
      default -> throw new IllegalArgumentException("Unsupported type: " + type);
    };
  }

  private static Variable var(String v) {
    return new Variable(v);
  }

  private record RpcType(Variable rpcName, List<Field> requestFields, List<Field> responseFields) {}

  private record Field(Variable fieldName, Variable fieldType, int fieldOffset) {}

  private record Variable(String value) {

    Variable {
      if (!value.matches("^[a-z][a-zA-Z0-9]*$")) {
        throw new IllegalArgumentException("expected" + value + " to be in camel case");
      }
    }

    private static String[] splitWords(String input) {
      return input.split("(?<!^)(?=[A-Z])");
    }

    private static String capitalize(String s) {
      return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    String toCamelCase() {
      String[] words = splitWords(value);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < words.length; i++) {
        String w = words[i].toLowerCase();
        sb.append(i == 0 ? w : capitalize(w));
      }
      return sb.toString();
    }

    String toPascalCase() {
      return Arrays.stream(splitWords(value))
          .map(w -> capitalize(w.toLowerCase()))
          .collect(Collectors.joining());
    }

    String toSnakeCaseUpper() {
      return Arrays.stream(splitWords(value))
          .map(String::toUpperCase)
          .collect(Collectors.joining("_"));
    }
  }
}
