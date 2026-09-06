package stoufexis.jarpc.gen;

import static stoufexis.jarpc.gen.Util.javaFile;
import static stoufexis.jarpc.gen.Util.path;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import stoufexis.jarpc.gen.model.*;

public final class Generator {

  public static List<OutputFile> generate(Spec spec) throws IOException {
    LinkedList<OutputFile> outputs = new LinkedList<>();

    for (TemplatePath template : templates) {
      outputs.addAll(generate(spec, template));
    }

    return List.copyOf(outputs);
  }

  public record OutputFile(String dir, String relativePath, String content) {}

  private static List<OutputFile> generate(Spec spec, TemplatePath path) throws IOException {
    ParsedTemplate root = ParsedTemplate.parseResource(path.getPath());
    ParsedTemplate.ForEachTypeBlock global = root.globalTypeBlock();

    if (global == null) {
      return List.of(outputFile(path.subdir, spec.serviceName(), path.name, root.fill(spec)));
    }

    LinkedList<OutputFile> output = new LinkedList<>();
    for (RpcType typ : spec.types()) {
      output.add(outputFile(path.subdir, typ.rpcName(), path.name, global.fillAsRoot(spec, typ)));
    }

    return List.copyOf(output);
  }

  private static OutputFile outputFile(
      String dir, Variable filePrefix, String fileSuffix, String content) {
    return new OutputFile(
        path(dir), javaFile(dir, filePrefix.toPascalCase() + fileSuffix), content);
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
      return javaFile("template", subdir, name);
    }
  }
}
