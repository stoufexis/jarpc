package stoufexis.jarpc.gen.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

  public static ParsedTemplate parse(String template) {
    Cursor c = Cursor.create(template);

    if (c.isEmpty()) throw new IllegalArgumentException("Empty template");

    LinkedList<RootComponent> blocks = new LinkedList<>();
    StringBuilder builder = new StringBuilder();

    for (; ; ) {

      if (c.exhausted()) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
        }
        break;
      }

      String line = c.read();

      if (foreachType(line)) {
        if (!builder.isEmpty()) {
          blocks.add(new PlainBlock(builder.toString()));
          builder = new StringBuilder();
        }

        blocks.add(parseForEachTypeBlock(c));
      } else {
        builder.append(line);
        builder.append("\n");
      }

      c.advance();
    }

    return new ParsedTemplate(List.copyOf(blocks));
  }

  private static ForEachTypeBlock parseForEachTypeBlock(Cursor c) {
    LinkedList<ForEachTypeComponent> blocks = new LinkedList<>();
    StringBuilder builder = new StringBuilder();

    c.advance();

    for (; ; ) {
      String line = c.read();

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

        blocks.add(parseRequestFieldBlock(c, foreachReqField(line)));
      } else {
        builder.append(line);
        builder.append("\n");
      }

      c.advance();
    }

    return new ForEachTypeBlock(List.copyOf(blocks));
  }

  private static ForEachTypeComponent parseRequestFieldBlock(Cursor c, boolean req) {
    StringBuilder builder = new StringBuilder();

    c.advance();

    for (; ; ) {
      String line = c.read();

      if (foreachReqField(line) || foreachResField(line)) break;

      builder.append(line);
      builder.append("\n");
      c.advance();
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

  private static class Cursor {
    private ArrayList<String> list;
    private int index;

    boolean exhausted() {
      return index >= list.size();
    }

    boolean isEmpty() {
      return list.isEmpty();
    }

    void advance() {
      index++;
    }

    String read() {
      return list.get(index);
    }

    static Cursor create(String template) {
      var pi = new Cursor();
      pi.index = 0;
      pi.list = new ArrayList<>(List.of(template.split("\n")));
      return pi;
    }
  }
}
