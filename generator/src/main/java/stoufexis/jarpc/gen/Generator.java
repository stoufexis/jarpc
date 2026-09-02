package stoufexis.jarpc.gen;

import stoufexis.jarpc.gen.model.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public final class Generator {

  public static List<OutputFile> generate(Spec spec) throws IOException {
    LinkedList<OutputFile> outputs = new LinkedList<>();

    for (TemplatePath template : templates) {
      outputs.addAll(generate(spec, template));
    }

    return List.copyOf(outputs);
  }

  public record OutputFile(String dir, String relativePath, String content) {}

  /** returns a list of file contents */
  private static List<OutputFile> generate(Spec spec, TemplatePath path) throws IOException {
    ParsedTemplate root = ParsedTemplate.parseResource(path.getPath());
    ParsedTemplate.ForEachTypeBlock globalTypeBlock = root.globalTypeBlock();

    if (globalTypeBlock != null) {
      LinkedList<OutputFile> output = new LinkedList<>();

      for (RpcType typ : spec.types()) {
        String relativePath =
            "/" + path.subdir + "/" + typ.rpcName().toPascalCase() + path.name + ".java";

        output.add(
            new OutputFile("/" + path.subdir, relativePath, globalTypeBlock.fillAsRoot(spec, typ)));
      }

      return List.copyOf(output);
    }

    String relativePath =
        "/" + path.subdir + "/" + spec.serviceName().toPascalCase() + path.name + ".java";

    return List.of(new OutputFile("/" + path.subdir, relativePath, root.fill(spec)));
  }

  private static final List<TemplatePath> templates =
      List.of(
          new TemplatePath("client", "SingleThreadedClient"),
          new TemplatePath("client", "SingleThreadedJarpcClient"),
          new TemplatePath("common", "Metadata"),
          new TemplatePath("common", "RequestDecode"),
          new TemplatePath("common", "RequestEncode"),
          new TemplatePath("common", "ResponseDecode"),
          new TemplatePath("common", "ResponseEncode"),
          new TemplatePath("server", "SingleThreadedJarpcServer"),
          new TemplatePath("server", "SingleThreadedServer"),
          new TemplatePath("server", "SingleThreadedStateMachine"),
          new TemplatePath("common", "RequestScratch"),
          new TemplatePath("client", "ConcurrentClient"),
          new TemplatePath("client", "ConcurrentJarpcClient"));

  private record TemplatePath(String subdir, String name) {
    String getPath() {
      return "/template/" + subdir + "/" + name + ".java";
    }
  }
}
