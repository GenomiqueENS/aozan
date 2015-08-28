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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.illumina.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.Globals.DEFAULT_CHARSET;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a reader for Casava design CSV files.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class CasavaDesignCSVReader extends AbstractCasavaDesignTextReader {

  private final BufferedReader reader;

  public SampleSheet readForQCReport(String version, final int laneCount)
      throws IOException, AozanException {

    setCompatibleForQCReport(true);
    setLaneCount(laneCount);

    return read(version);
  }

  @Override
  public SampleSheet read(final String version) throws IOException,
      AozanException {

    String line = null;

    while ((line = this.reader.readLine()) != null) {

      line = line.trim();
      if ("".equals(line)) {
        continue;
      }

      try {

        // Parse the line
        parseLine(SampleSheetUtils.parseCSVDesignLine(line), version);

      } catch (AozanException e) {

        // If an error occurs while parsing add the line to the exception
        // message
        throw new IOException(e.getMessage() + " in line: " + line);
      }
    }

    this.reader.close();

    return getDesign();
  }

  /**
   * Inits the reader and check file is valid.
   * @param file the file
   * @return the buffered reader
   * @throws FileNotFoundException if file not exist or is empty
   */
  private BufferedReader initReader(final File file)
      throws FileNotFoundException {

    checkNotNull(file, "sample sheet");

    if (!file.isFile()) {
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());
    }

    if (file.length() == 0) {
      throw new FileNotFoundException("File is empty: "
          + file.getAbsolutePath());
    }

    return FileUtils.createBufferedReader(file);
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public CasavaDesignCSVReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader =
        new BufferedReader(new InputStreamReader(is, DEFAULT_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public CasavaDesignCSVReader(final File file) throws FileNotFoundException {

    this.reader = initReader(file);
  }

  /**
   * Public constructor
   * @param filename File to use
   */
  public CasavaDesignCSVReader(final String filename)
      throws FileNotFoundException {

    if (filename == null) {
      throw new NullPointerException("Filename is null");
    }

    this.reader = initReader(new File(filename));
  }

}
