package stoufexis.jarpc.gen;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Generator {

  public static void generate(
      String serviceName, String serviceSpec, List<String> packageComponents) {
    String pkg = String.join(".", packageComponents);
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

    List<RpcType> rpcTypes = List.copyOf(types);
  }

  private static void generateCommon(Spec args) throws IOException {
    String base = "/template/common/";
    String metadataTemplate = "Metadata.template";
    List<String> codecTemplates =
        List.of(
            "RequestDecode.template",
            "RequestEncode.template",
            "RequestScratch.template",
            "ResponseDecode.template",
            "ResponseEncode.template",
            "ResponseScratch.template");

    LinkedList<String> foreachContext = new LinkedList<>();
    for (String line : Files.readAllLines(Path.of(base + metadataTemplate))) {}
  }

  private static int messageSize(List<Field> fields) {
    int size = 0;
    for (var field : fields) {
      size += typeLength(field.fieldType.value);
    }

    return size;
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

  private record Spec(Variable serviceName, String pkg, String outputPath, List<RpcType> spec) {
    String valueForPlaceholder(String placeholder) {
      return switch (placeholder) {
        case "package" -> pkg;
        case "Service" -> serviceName.toPascalCase();
        case "service" -> serviceName.toCamelCase();
        case "SERVICE" -> serviceName.toSnakeCaseUpper();
        default ->
            throw new IllegalArgumentException("Unknown top-level placeholder " + placeholder);
      };
    }
  }

  private static Variable var(String v) {
    return new Variable(v);
  }

  private record RpcType(
      Variable rpcName,
      List<Field> requestFields,
      List<Field> responseFields,
      int requestSize,
      int responseSize,
      int typeId) {
    String valueForPlaceholder(String placeholder) {
      return switch (placeholder) {
        case "type" -> rpcName.toCamelCase();
        case "Type" -> rpcName.toPascalCase();
        case "TYPE" -> rpcName.toSnakeCaseUpper();
        default ->
            throw new IllegalArgumentException("Unknown per-type placeholder " + placeholder);
      };
    }
  }

  private record Field(Variable fieldName, Variable fieldType, int fieldOffset) {
    String valueForPlaceholder(String placeholder) {
      return switch (placeholder) {
        case "Field" -> fieldName.toPascalCase();
        case "field" -> fieldName.toCamelCase();
        case "FIELD" -> fieldName.toSnakeCaseUpper();
        default ->
            throw new IllegalArgumentException("Unknown per-type placeholder " + placeholder);
      };
    }

  }

  private record Variable(String value) {

    Variable {
      if (!value.matches("^[a-z][a-zA-Z0-9]*$")) {
        throw new IllegalArgumentException("expected " + value + " to be in camel case");
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
