/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.illumina.samplesheet.io;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;

/**
 * This class define a writer for Bcl2fastq CSV samplesheet files.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class SampleSheetCSVWriter implements SampleSheetWriter, AutoCloseable {

  private final Writer writer;
  private boolean addCommas;
  private int version = 2;

  @Override
  public void writer(final SampleSheet samplesheet) throws IOException {

    final String text;

    switch (this.version) {

    case 1:
      text = SampleSheetUtils.toSampleSheetV1CSV(samplesheet);

      break;

    case 2:
      text = SampleSheetUtils.toSampleSheetV2CSV(samplesheet);
      break;

    default:
      throw new IOException(
          "Unknown bcl2fastq samplesheet format version: " + this.version);
    }

    this.writer.write(this.addCommas ? addMissingComas(text) : text);
  }

  /**
   * Set the version of the samplesheet file to read.
   * @param version the version of the samplesheet file to read
   */
  public void setVersion(final int version) {

    this.version = version;
  }

  /**
   * Get the version of the samplesheet file to read.
   * @return the version of the samplesheet file to read
   */
  public int getVersion() {

    return this.version;
  }

  /**
   * Add missing commas, to get a similar output as a spreadsheet CSV export.
   * @param addCommas if true additional commas will be added to end of lines
   */
  public void addMissingCommas(boolean addCommas) {

    this.addCommas = addCommas;
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  //
  // Utility methods
  //

  private static int maxCharacterInLines(String text) {

    requireNonNull(text);

    int max = 0;

    for (String s : text.split("\\n")) {
      max = Math.max(max, commasInLine(s));
    }

    return max;
  }

  private static int commasInLine(String line) {

    requireNonNull(line);

    return line.length() - line.replace(",", "").length();
  }

  private static String addMissingComas(String text) {

    requireNonNull(text);

    int max = maxCharacterInLines(text);
    StringBuilder sb = new StringBuilder();

    for (String s : text.split("\\n")) {

      sb.append(s);

      for (int i = commasInLine(s); i < max; i++) {
        sb.append(',');
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public SampleSheetCSVWriter(final Writer writer) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public SampleSheetCSVWriter(final OutputStream os)
      throws FileNotFoundException {

    this.writer = new OutputStreamWriter(os);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public SampleSheetCSVWriter(final File outputFile) throws IOException {

    this.writer = new FileWriter(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public SampleSheetCSVWriter(final String outputFilename) throws IOException {

    this.writer = new FileWriter(outputFilename);
  }

}
