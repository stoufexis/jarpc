package stoufexis.jarpc.gen;

import stoufexis.jarpc.gen.model.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class Cli {
  static void main(String[] args) throws IOException {
    Path specPath = null;
    String serviceName = null;
    String packageName = null;
    String output = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--spec", "-s" -> specPath = Path.of(args[++i]);
        case "--name", "-n" -> serviceName = args[++i];
        case "--package", "-p" -> packageName = args[++i];
        case "--output", "-o" -> output = args[++i];
        case "--help", "-h" -> {
          System.out.println(
              "Usage: --spec <json file> --name <string> --package <string> --output <dir>");
          return;
        }
        default -> {
          System.out.println("Unknown argument: " + args[i]);
          return;
        }
      }
    }

    Objects.requireNonNull(specPath, "spec path not provided");
    Objects.requireNonNull(serviceName, "service name not provided");
    Objects.requireNonNull(packageName, "package path not provided");
    Objects.requireNonNull(output, "output path not provided");

    Spec spec = Spec.parse(serviceName, Files.readString(specPath), packageName);

    for (Generator.OutputFile file : Generator.generate(spec)) {
      Path path = Path.of(output + file.relativePath());
      Files.deleteIfExists(path);
      Files.createDirectories(Path.of(output + file.dir()));
      Files.writeString(path, file.content(), StandardOpenOption.CREATE_NEW);
    }
  }
}
