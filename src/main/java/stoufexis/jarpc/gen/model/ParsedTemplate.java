package stoufexis.jarpc.gen.model;

import java.util.List;

public record ParsedTemplate(List<RootComponent> components) {
  public String fill(Spec spec) {
    StringBuilder output = new StringBuilder();

    for (RootComponent c : components) {
      output.append(c.fill(spec));
      output.append("\n");
    }

    return spec.replacements().applyTo(output.toString());
  }

  public static void parse(List<String> templateLines) {

  }

  private static boolean foreachType(String line) {
    return line.replace(" ", "").contains("///foreachType");
  }

  private static boolean foreachReqField(String line) {
    return line.replace(" ", "").contains("///foreachRequestField");
  }

  private static boolean foreachResField(String line) {
    return line.replace(" ", "").contains("///foreachResponseField");
  }

  private sealed interface RootComponent {
    String fill(Spec spec);
  }

  private record PlainBlock(String block) implements RootComponent, ForEachTypeComponent {
    @Override
    public String fill(Spec spec) {
      return this.block;
    }

    @Override
    public String fill(RpcType type) {
      return this.block;
    }
  }

  private record ForEachTypeBlock(List<ForEachTypeComponent> block) implements RootComponent {
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

  private sealed interface ForEachTypeComponent {
    String fill(RpcType type);
  }

  private record ForEachRequestFieldBlock(String block) implements ForEachTypeComponent {
    @Override
    public String fill(RpcType type) {
      return type.foreachRequestField(block);
    }
  }

  private record ForEachResponse(String block) implements ForEachTypeComponent {
    @Override
    public String fill(RpcType type) {
      return type.foreachResponseField(block);
    }
  }
}
