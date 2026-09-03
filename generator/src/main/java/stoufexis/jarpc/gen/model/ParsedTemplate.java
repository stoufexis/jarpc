package stoufexis.jarpc.gen.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public record ParsedTemplate(List<RootComponent> block) {
  public ForEachTypeBlock globalTypeBlock() {
    return switch (block.getFirst()) {
      case ForEachTypeBlock forEachTypeBlock when block.size() == 1 -> forEachTypeBlock;
      default -> null;
    };
  }

  public String fill(Spec spec) {
    StringBuilder output = new StringBuilder();

    for (RootComponent c : block) {
      output.append(c.fill(spec));
      output.append("\n");
    }

    return spec.replacements().applyTo(output.toString());
  }

  public static ParsedTemplate parseResource(String resourcePath) throws IOException {
    return ParsedTemplate.parse(
        new String(
            Objects.requireNonNull(ParsedTemplate.class.getResourceAsStream(resourcePath))
                .readAllBytes(),
            StandardCharsets.UTF_8));
  }

  public static ParsedTemplate parse(String template) {
    Iterator<String> iter = template.lines().iterator();
    LinkedList<RootComponent> blocks = new LinkedList<>();
    StringBuilder builder = new StringBuilder();

    while (iter.hasNext()) {
      String line = iter.next();

      if (containsForeachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(plain(builder));
          builder = new StringBuilder();
        }
        blocks.add(parseForEachTypeBlock(iter));

      } else {
        builder.append(line);
        builder.append("\n");
      }
    }

    if (!builder.isEmpty()) blocks.add(plain(builder));

    return new ParsedTemplate(List.copyOf(blocks));
  }

  private static ForEachTypeBlock parseForEachTypeBlock(Iterator<String> iter) {
    LinkedList<ForEachTypeComponent> blocks = new LinkedList<>();
    StringBuilder builder = new StringBuilder();

    while (iter.hasNext()) {
      String line = iter.next();

      if (containsForeachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(plain(builder));
        }
        break;

      } else if (containsForeachReqField(line) || containsForeachResField(line)) {
        if (!builder.isEmpty()) {
          blocks.add(plain(builder));
          builder = new StringBuilder();
        }
        blocks.add(parseRequestFieldBlock(iter, containsForeachReqField(line)));

      } else {
        builder.append(line);
        builder.append("\n");
      }
    }

    return new ForEachTypeBlock(List.copyOf(blocks));
  }

  private static ForEachTypeComponent parseRequestFieldBlock(Iterator<String> iter, boolean req) {
    StringBuilder builder = new StringBuilder();

    while (iter.hasNext()) {
      String line = iter.next();
      if (containsForeachReqField(line) || containsForeachResField(line)) break;
      builder.append(line);
      builder.append("\n");
    }

    return req ? foreachRequest(builder) : foreachResponse(builder);
  }

  private static boolean containsForeachType(String line) {
    return line.replace(" ", "").contains("///foreachType");
  }

  private static boolean containsForeachReqField(String line) {
    return line.replace(" ", "").contains("///foreachRequestField");
  }

  private static boolean containsForeachResField(String line) {
    return line.replace(" ", "").contains("///foreachResponseField");
  }

  private static PlainBlock plain(StringBuilder builder) {
    return new PlainBlock(builder.toString());
  }

  private static ForEachRequestFieldBlock foreachRequest(StringBuilder builder) {
    return new ForEachRequestFieldBlock(builder.toString());
  }

  private static ForEachResponseFieldBlock foreachResponse(StringBuilder builder) {
    return new ForEachResponseFieldBlock(builder.toString());
  }

  public sealed interface RootComponent {
    String fill(Spec spec);
  }

  public record PlainBlock(String block) implements RootComponent, ForEachTypeComponent {
    @Override
    public String fill(Spec spec) {
      return this.block;
    }

    @Override
    public String fill(RpcType type) {
      return this.block;
    }
  }

  public record ForEachTypeBlock(List<ForEachTypeComponent> block) implements RootComponent {
    @Override
    public String fill(Spec spec) {
      StringBuilder output = new StringBuilder();

      for (RpcType typ : spec.types()) {
        StringBuilder perType = new StringBuilder();

        for (ForEachTypeComponent c : block) {
          perType.append(c.fill(typ));
        }

        output.append(typ.replacements().applyTo(perType.toString()));
      }

      return output.toString();
    }

    public String fillAsRoot(Spec spec, RpcType typ) {
      StringBuilder perType = new StringBuilder();

      for (ForEachTypeComponent c : block) {
        perType.append(c.fill(typ));
        perType.append("\n");
      }

      return spec.replacements().applyTo(typ.replacements().applyTo(perType.toString()));
    }
  }

  public sealed interface ForEachTypeComponent {
    String fill(RpcType type);
  }

  public record ForEachRequestFieldBlock(String block) implements ForEachTypeComponent {
    @Override
    public String fill(RpcType type) {
      return type.foreachRequestField(block);
    }
  }

  public record ForEachResponseFieldBlock(String block) implements ForEachTypeComponent {
    @Override
    public String fill(RpcType type) {
      return type.foreachResponseField(block);
    }
  }
}
