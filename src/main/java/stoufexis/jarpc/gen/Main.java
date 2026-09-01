package stoufexis.jarpc.gen;

import stoufexis.jarpc.gen.model.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {
  static void main(String[] args) throws IOException {
    Spec spec =
        Spec.parse(
            "exchange",
            Files.readString(Path.of("/home/stoufexis/dev/jarpc/example/exchange.json")),
            "stoufexis.jarpc.example");

    for (Generator.OutputFile file : Generator.generate(spec)) {
      Path path =
          Path.of(
              "/home/stoufexis/dev/jarpc/src/main/java/stoufexis/jarpc/example/"
                  + file.relativePath());
      Files.deleteIfExists(path);
      Files.createDirectories(
          Path.of("/home/stoufexis/dev/jarpc/src/main/java/stoufexis/jarpc/example/" + file.dir()));
      Files.writeString(path, file.content(), StandardOpenOption.CREATE_NEW);
    }
  }
}
