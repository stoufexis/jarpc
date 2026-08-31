package stoufexis.jarpc.gen.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public record ParsedTemplate(List<RootComponent> block) {
  public String fill(Spec spec) {
    StringBuilder output = new StringBuilder();

    for (RootComponent c : block) {
      output.append(c.fill(spec));
      output.append("\n");
    }

    return spec.replacements().applyTo(output.toString());
  }

  public static ParsedTemplate parse(List<String> templateLines) {
    if (templateLines.isEmpty()) throw new IllegalArgumentException("Empty template");

    ArrayList<String> list = new ArrayList<>(templateLines);
    LinkedList<RootComponent> blocks = new LinkedList<>();

    StringBuilder builder = new StringBuilder();

    int startAt = 0;

    for (; ; ) {

      if (startAt >= list.size()) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
        }
        break;
      }

      String line = list.get(startAt);

      if (foreachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
          builder = new StringBuilder();
        }

        int[] out = new int[1];
        blocks.add(parseForEachTypeBlock(list, startAt, out));
        startAt = out[0];
      } else {
        builder.append(line);
        builder.append("\n");
      }

      startAt++;
    }

    return new ParsedTemplate(List.copyOf(blocks));
  }

  @Override
  public String toString() {
    return block.stream().map(Object::toString).collect(Collectors.joining("\n"));
  }

  private static ForEachTypeBlock parseForEachTypeBlock(
      ArrayList<String> list, int startAt, int[] newIndex) {

    LinkedList<ForEachTypeComponent> blocks = new LinkedList<>();

    StringBuilder builder = new StringBuilder();

    startAt += 1;

    for (; ; ) {
      String line = list.get(startAt);

      if (foreachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
        }
        break;

      } else if (foreachReqField(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
          builder = new StringBuilder();
        }

        int[] out = new int[1];
        blocks.add(parseRequestFieldBlock(list, startAt, out));
        startAt = out[0];

      } else if (foreachResField(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
          builder = new StringBuilder();
        }

        int[] out = new int[1];
        blocks.add(parseResponseFieldBlock(list, startAt, out));
        startAt = out[0];

      } else {
        builder.append(line);
        builder.append("\n");
      }

      startAt++;
    }

    newIndex[0] = startAt;
    return new ForEachTypeBlock(List.copyOf(blocks));
  }

  private static ForEachRequestFieldBlock parseRequestFieldBlock(
      ArrayList<String> list, int startAt, int[] newIndex) {
    StringBuilder builder = new StringBuilder();

    startAt += 1;

    for (; ; ) {
      String line = list.get(startAt);

      if (foreachReqField(line)) break;

      builder.append(line);
      builder.append("\n");
      startAt++;
    }

    newIndex[0] = startAt;
    return new ForEachRequestFieldBlock(builder.toString());
  }

  private static ForEachResponseFieldBlock parseResponseFieldBlock(
      ArrayList<String> list, int startAt, int[] newIndex) {
    StringBuilder builder = new StringBuilder();

    startAt += 1;

    for (; ; ) {
      String line = list.get(startAt);

      if (foreachResField(line)) break;

      builder.append(line);
      builder.append("\n");
      startAt++;
    }

    newIndex[0] = startAt;
    return new ForEachResponseFieldBlock(builder.toString());
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

    @Override
    public String toString() {
      return block;
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
        }

        output.append(typ.replacements().applyTo(perType.toString()));
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

    @Override
    public String toString() {
      return "/// foreachTypeBlock\n"
          + block.stream().map(Object::toString).collect(Collectors.joining("\n"))
          + "/// foreachTypeBlock\n";
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

    @Override
    public String toString() {
      return "/// foreachRequestFieldBlock\n" + block + "/// foreachRequestFieldBlock\n";
    }
  }

  private record ForEachResponseFieldBlock(String block) implements ForEachTypeComponent {
    @Override
    public String fill(RpcType type) {
      return type.foreachResponseField(block);
    }

    @Override
    public String toString() {
      return "/// foreachResponseFieldBlock\n" + block + "/// foreachResponseFieldBlock\n";
    }
  }
}
