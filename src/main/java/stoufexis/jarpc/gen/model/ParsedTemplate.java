package stoufexis.jarpc.gen.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record ParsedTemplate(List<RootComponent> block) {
  public ForEachTypeBlock globalTypeBlock() {
    if (block.size() != 1) return null;

    switch (block.getFirst()) {
      case ForEachTypeBlock forEachTypeBlock -> {
        return forEachTypeBlock;
      }

      case PlainBlock _ -> {
        return null;
      }
    }
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

      if (foreachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
          builder = new StringBuilder();
        }

        blocks.add(parseForEachTypeBlock(iter));
      } else {
        builder.append(line);
        builder.append("\n");
      }
    }

    if (!builder.isEmpty()) {
      blocks.add(new PlainBlock(builder.toString()));
    }

    return new ParsedTemplate(List.copyOf(blocks));
  }

  private static ForEachTypeBlock parseForEachTypeBlock(Iterator<String> iter) {
    LinkedList<ForEachTypeComponent> blocks = new LinkedList<>();
    StringBuilder builder = new StringBuilder();

    while (iter.hasNext()) {
      String line = iter.next();

      if (foreachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
        }
        break;

      } else if (foreachReqField(line) || foreachResField(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
          builder = new StringBuilder();
        }

        blocks.add(parseRequestFieldBlock(iter, foreachReqField(line)));
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

      if (foreachReqField(line) || foreachResField(line)) break;

      builder.append(line);
      builder.append("\n");
    }

    return req
        ? new ForEachRequestFieldBlock(builder.toString())
        : new ForEachResponseFieldBlock(builder.toString());
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

  @Override
  public String toString() {
    return block.stream().map(Object::toString).collect(Collectors.joining("\n"));
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

    @Override
    public String toString() {
      return block;
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

    @Override
    public String toString() {
      return "/// foreachType\n"
          + block.stream().map(Object::toString).collect(Collectors.joining("\n"))
          + "/// foreachType\n";
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

    @Override
    public String toString() {
      return "/// foreachRequestField\n" + block + "/// foreachRequestField\n";
    }
  }

  public record ForEachResponseFieldBlock(String block) implements ForEachTypeComponent {
    @Override
    public String fill(RpcType type) {
      return type.foreachResponseField(block);
    }

    @Override
    public String toString() {
      return "/// foreachResponseField\n" + block + "/// foreachResponseField\n";
    }
  }
}
