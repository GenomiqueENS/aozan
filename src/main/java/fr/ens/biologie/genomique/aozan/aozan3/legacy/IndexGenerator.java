package fr.ens.biologie.genomique.aozan.aozan3.legacy;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.aozan3.Globals;

/**
 * This class allow to generate index file like for Aozan 1.x and 2.x.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IndexGenerator {

  /**
   * Create index.html file in legacy mode.
   * @param outputDir output directory
   * @param runId run id
   * @param sections sections to enable
   * @throws AozanException if an error occurs while creating HTML index file
   */
  public static void createIndexRun(Path outputDir, String runId,
      Collection<String> sections) throws AozanException {

    requireNonNull(outputDir);
    requireNonNull(runId);
    requireNonNull(sections);

    File dir = outputDir.toFile();
    File indexFile = new File(dir, "index.html");

    List<String> lines = new BufferedReader(new InputStreamReader(
        IndexGenerator.class.getResourceAsStream("/template_index_run.html"),
        StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

    boolean inSection = false;
    boolean printSection = false;

    StringBuilder sb = new StringBuilder();
    for (String line : lines) {

      line = line.replace("${RUN_ID}", runId)
          .replace("${APP_NAME}", Globals.APP_NAME)
          .replace("${VERSION}", Globals.APP_VERSION_STRING)
          .replace("${WEBSITE}", Globals.WEBSITE_URL);

      if (line.startsWith("<!--START_SECTION")) {
        inSection = true;

        List<String> fields =
            Splitter.on(' ').omitEmptyStrings().splitToList(line);
        printSection = fields.size() > 1 && sections.contains(fields.get(1));

        continue;

      } else if (line.startsWith("<!--END_SECTION")) {
        inSection = false;
        continue;
      }

      if (!inSection || (inSection && printSection)) {

        if (line.startsWith("<li><a href=\"")) {

          List<String> fields =
              Splitter.on('"').omitEmptyStrings().splitToList(line);

          File f = new File(dir, fields.get(1));
          if (!f.exists()) {
            continue;
          }

        }

        sb.append(line);
        sb.append('\n');
      }
    }

    // Write final index file
    try {
      Files.write(indexFile.toPath(),
          sb.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new AozanException("Unable to write HTML index file", e);
    }
  }

}
