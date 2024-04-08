package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.kenetre.util.StringUtils.sizeToHumanReadable;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.TemplateEmailMessage;

/**
 * This class define a template email message for end of step messages.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class DataProcessorTemplateEmailMessage extends TemplateEmailMessage {

  /**
   * Create an end of step message.
   * @param subject email message
   * @param runId run ID
   * @param outputDirectory output directory of the step
   * @param startTime start time of the step
   * @param endTime end time of the step
   * @param outputSize output size of generated data
   * @param diskFree disk free space available at the end of the steo
   * @param variables variables for the template
   * @return an email message
   * @throws Aozan3Exception if an error occurs while creating the message
   */
  public EmailMessage endDataProcessorEmail(String subject, RunId runId,
      Path outputDirectory, long startTime, long endTime, long outputSize,
      long diskFree, Map<String, String> variables) throws Aozan3Exception {

    requireNonNull(runId);

    Map<String, String> map =
        variables != null ? new HashMap<>(variables) : new HashMap<>();

    map.put("run_id", runId.getId());
    map.put("current_date", new Date(System.currentTimeMillis()).toString());
    map.put("start_date", new Date(startTime).toString());
    map.put("end_date", new Date(endTime).toString());
    map.put("duration", toTimeHumanReadable(endTime - startTime));

    if (outputDirectory != null) {
      map.put("output_directory", outputDirectory.toString());
    }
    map.put("output_size", sizeToHumanReadable(outputSize));
    map.put("disk_free", sizeToHumanReadable(diskFree));

    return toEmailMessage(subject, map);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf Configuration
   * @param templateKey template configuration key
   * @param defaultTemplateResource default path for the template in Java
   *          resources
   * @throws IOException if an error occurs while reading the template file
   */
  public DataProcessorTemplateEmailMessage(Configuration conf,
      String templateKey, String defaultTemplateResource) throws IOException {

    super(conf);
    if (conf.containsKey(templateKey)) {
      // Load template from file
      setTemplateFromFile(conf.getPath(conf.get(templateKey)));
    } else {
      // Load template from resource
      setTemplateFromResource(defaultTemplateResource);
    }
  }

}
