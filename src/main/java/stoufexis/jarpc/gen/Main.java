package stoufexis.jarpc.gen;

import stoufexis.jarpc.gen.model.ParsedTemplate;
import stoufexis.jarpc.gen.model.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  static void main() throws IOException {

    Spec spec = Spec.parse(
        "exchange",
        Files.readString(Path.of("/home/stoufexis/dev/jarpc/example/exchange.json")),
        "com.stoufexis.exchange");

    ParsedTemplate template =
        ParsedTemplate.parse(
            Files.readAllLines(
                Path.of(
                    "/home/stoufexis/dev/jarpc/src/main/resources/template/client/SingleThreadedJarpcClient.java")));

    System.out.println(template.fill(spec));
  }
}
