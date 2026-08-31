package stoufexis.jarpc.gen;

import org.json.JSONArray;
import org.json.JSONObject;
import stoufexis.jarpc.gen.model.*;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public final class Generator {

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

  /** returns a list of file contents */
  private static List<String> generate(Spec args, TemplateRoot root) {
    if (root.components().isEmpty()) throw new IllegalArgumentException("empty template");

    RootComponent first = root.components.getFirst();

    switch (first) {
      case RootComponent.ForEachTypeBlock c when root.components.size() == 1 -> {
        LinkedList<String> output = new LinkedList<>();

        for (RpcType typ : args.spec()) {
          output.add(c.fill(typ));
        }

        return List.copyOf(output);
      }

      case RootComponent.PlainBlock _ -> {
        return List.of(root.fill(args));
      }

      default -> throw new IllegalArgumentException("Unexpected template shape");
    }
  }

  private static int messageSize(List<Field> fields) {
    int size = 0;
    for (var field : fields) {
      size += typeLength(field.fieldType().value());
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

  private static final List<TemplatePath> templates =
      List.of(
          new TemplatePath("client", "/template/client/ConcurrentClient.java"),
          new TemplatePath("client", "/template/client/ConcurrentJarpcClient.java"),
          new TemplatePath("client", "/template/client/SingleThreadedClient.java"),
          new TemplatePath("client", "/template/client/SingleThreadedJarpcClient.java"),
          new TemplatePath("common", "/template/common/Metadata.java"),
          new TemplatePath("common", "/template/common/RequestDecode.java"),
          new TemplatePath("common", "/template/common/RequestEncode.java"),
          new TemplatePath("common", "/template/common/RequestScratch.java"),
          new TemplatePath("common", "/template/common/ResponseDecode.java"),
          new TemplatePath("common", "/template/common/ResponseEncode.java"),
          new TemplatePath("common", "/template/common/ResponseScratch.java"),
          new TemplatePath("server", "/template/server/ConcurrentJarpcServer.java"),
          new TemplatePath("server", "/template/server/ConcurrentServer.java"),
          new TemplatePath("server", "/template/server/SingleThreadedJarpcServer.java"),
          new TemplatePath("server", "/template/server/SingleThreadedServer.java"));

  private record TemplatePath(String subdir, Path path) {
    TemplatePath(String subdir, String path) {
      this(subdir, Path.of(path));
    }
  }

  private static Variable var(String v) {
    return new Variable(v);
  }

  private record TemplateRoot(List<RootComponent> components) {
    String fill(Spec spec) {
      StringBuilder output = new StringBuilder();

      for (RootComponent c : components) {
        output.append(c.fill(spec));
        output.append("\n");
      }

      return spec.replacements().applyTo(output.toString());
    }
  }

  private sealed interface RootComponent {
    String fill(Spec spec);

    record PlainBlock(String block) implements RootComponent {
      @Override
      public String fill(Spec spec) {
        return this.block;
      }
    }

    record ForEachTypeBlock(List<ForEachTypeComponent> block) implements RootComponent {
      @Override
      public String fill(Spec spec) {
        StringBuilder output = new StringBuilder();

        for (RpcType typ : spec.spec()) {
          StringBuilder perType = new StringBuilder();

          for (ForEachTypeComponent c : block) {
            perType.append(c.fill(typ));
            perType.append("\n");
          }

          output.append(typ.replacements().applyTo(perType.toString()));
          perType.append("\n");
        }

        return output.toString();
      }

      public String fill(RpcType typ) {
        StringBuilder perType = new StringBuilder();

        for (ForEachTypeComponent c : block) {
          perType.append(c.fill(typ));
          perType.append("\n");
        }

        return typ.replacements().applyTo(perType.toString());
      }
    }
  }

  private sealed interface ForEachTypeComponent {
    String fill(RpcType type);

    record PlainBlock(String block) implements ForEachTypeComponent {
      @Override
      public String fill(RpcType type) {
        return this.block;
      }
    }

    record ForEachRequestFieldBlock(String block) implements ForEachTypeComponent {
      @Override
      public String fill(RpcType type) {
        return type.foreachRequestField(block);
      }
    }

    record ForEachResponse(String block) implements ForEachTypeComponent {
      @Override
      public String fill(RpcType type) {
        return type.foreachResponseField(block);
      }
    }
  }
}
