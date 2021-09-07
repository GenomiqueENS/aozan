package fr.ens.biologie.genomique.aozan.aozan3;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Splitter;

/**
 * This class contents some utility methods for Illumina.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaUtils {

  /**
   * Check if the run id is valid.
   * @param runId the run identifier
   * @return true if the run identifier is valid
   */
  public static boolean checkRunId(String runId) {

    // TODO create unit test

    Objects.requireNonNull(runId);

    List<String> fields = Splitter.on('_').splitToList(runId);

    if (fields.size() != 4) {
      return false;
    }

    String date = fields.get(0);
    String count = fields.get(2);
    String flowCellId = fields.get(3);

    // Test the date
    if (!date.matches("^[0-9]*$") || date.length() != 6) {
      return false;
    }

    // Test the run count
    if (!count.matches("^[0-9]*$")) {
      return false;
    }

    // Test the flow cell id
    if (flowCellId.length() == 10 && flowCellId.matches("^[0-9a-zA-Z]*$")) {
      return true;
    }

    // Test the flow cell id
    if (flowCellId.length() == 9 && flowCellId.matches("^[0-9a-zA-Z]*$")) {
      return true;
    }

    if (flowCellId.length() == 15) {
      List<String> flowCellFields = Splitter.on('-').splitToList(flowCellId);

      if (flowCellFields.size() == 2
          && flowCellFields.get(0).equals("000000000")) {
        return true;
      }
    }

    return false;
  }

  //
  // Constructor
  //

  private IlluminaUtils() {
  }

}
