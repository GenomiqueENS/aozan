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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.illumina.samplesheet.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;

/**
 * This class define a reader for bcl2fastq CSV samplesheet files.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class SampleSheetCSVReader implements SampleSheetReader {

  /* Default Charset. */
  private static final Charset CHARSET = Charsets.UTF_8;

  private final BufferedReader reader;
  private int version = -1;

  @Override
  public SampleSheet read() throws IOException {

    final SampleSheetParser parser;

    switch (this.version) {

    case -1:
      parser = new SampleSheetDiscoverFormatParser();
      break;

    case 1:
      parser = new SampleSheetV1Parser();
      break;

    case 2:
      parser = new SampleSheetV2Parser();
      break;

    default:
      throw new IOException(
          "Unknown bcl2fastq samplesheet format version: " + this.version);
    }

    String line = null;

    while ((line = this.reader.readLine()) != null) {

      line = line.trim();
      if ("".equals(line)) {
        continue;
      }

      try {

        // Parse the line
        parser.parseLine(SampleSheetUtils.parseCSVDesignLine(line));
      } catch (IOException e) {

        // If an error occurs while parsing add the line to the exception
        // message
        throw new IOException(e.getMessage() + " in line: " + line);
      }
    }

    this.reader.close();

    return parser.getSampleSheet();
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

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public SampleSheetCSVReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   * @throws FileNotFoundException if the file does not exists
   */
  public SampleSheetCSVReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    if (!file.isFile()) {
      throw new FileNotFoundException(
          "File not found: " + file.getAbsolutePath());
    }

    this.reader = new BufferedReader(new FileReader(file));
  }

  /**
   * Public constructor
   * @param filename File to use
   */
  public SampleSheetCSVReader(final String filename)
      throws FileNotFoundException {

    if (filename == null) {
      throw new NullPointerException("Filename is null");
    }

    final File file = new File(filename);

    if (!file.isFile()) {
      throw new FileNotFoundException(
          "File not found: " + file.getAbsolutePath());
    }

    this.reader = new BufferedReader(new FileReader(file));
  }

}
