package stoufexis.jarpc.gen;

import stoufexis.jarpc.gen.model.ParsedTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  static void main() throws IOException {
    ParsedTemplate template =
        ParsedTemplate.parse(
            Files.readAllLines(
                Path.of(
                    "/home/stoufexis/dev/jarpc/src/main/resources/template/common/RequestScratch.java")));

    System.out.println(template);
  }
}
