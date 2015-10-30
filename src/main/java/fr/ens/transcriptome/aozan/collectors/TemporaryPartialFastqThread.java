/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 3 or
 * later and CeCILL. This should be distributed with the code.
 * If you do not have a copy, see:
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
 * or to join the Aozan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.logging.Logger;

import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.aozan.illumina.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * The class define a class for a thread that create a temporary partial fastq
 * file, with fixed reads (200 000) to use for contamination research. Only the
 * reads pf are used. They are selected among the first 30 millions reads pf.
 * @since 1.1
 * @author Sandrine Perrin
 */
public class TemporaryPartialFastqThread extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  // count reads pf necessary for create a temporary partial fastq
  private final int countReadsPFtoCopy;

  private final int rawClusterCount;
  private final int pfClusterCountParsed;

  private final File tmpFastqFile;
  private boolean uncompressFastqFile = false;

  @Override
  protected void notifyStartLogger() {
    // Nothing to log
  }

  @Override
  protected void process() throws AozanException {

    processResults();
  }

  @Override
  protected void notifyEndLogger(final String duration) {

    final String txt = this.uncompressFastqFile
        ? " by uncompressed fastq file "
        : " by created partial file ("
            + this.countReadsPFtoCopy + " selecting in "
            + this.pfClusterCountParsed;

    LOGGER.fine("Temporary Partial fastq created in "
        + duration + " for " + getFastqSample().getKeyFastqSample() + txt
        + ")");

  }

  /**
   * Create a temporary partial file from a array of fastq files.
   * @throws AozanException if an error occurs while creating file
   */
  @Override
  protected void processResults() throws AozanException {

    if (!getFastqStorage().getTemporaryFile(getFastqSample()).exists()) {

      if (this.countReadsPFtoCopy > this.rawClusterCount) {
        uncompressedFastqFile();
        this.uncompressFastqFile = true;

      } else if (this.countReadsPFtoCopy > this.pfClusterCountParsed) {
        // Use all reads
        partialFastqFile();
      } else {
        // Filter reads
        filteredFastqFile();
      }

      // Rename file: remove '.tmp' final
      if (!this.tmpFastqFile
          .renameTo(getFastqStorage().getTemporaryFile(getFastqSample()))) {
        LOGGER.warning("FastQC: fail to rename tmp fastq file "
            + this.tmpFastqFile.getAbsolutePath());
      }
    }
  }

  /**
   * Write the temporary partial file from a array of fastq files, only reads
   * passing filter Illumina are writing.
   * @throws AozanException if an error occurs while creating file
   */
  private void filteredFastqFile() throws AozanException {

    Writer fwTmpFastq = null;
    try {
      fwTmpFastq =
          Files.newWriter(this.tmpFastqFile, Globals.DEFAULT_FILE_ENCODING);

      final int step = (int) (1
          / ((double) this.countReadsPFtoCopy / this.pfClusterCountParsed));

      for (final File fastqFile : getFastqSample().getFastqFiles()) {
        int comptReadsPF = 1;
        int countReadsToCopyByFastq =
            this.countReadsPFtoCopy / getFastqSample().getFastqFiles().size();

        if (!fastqFile.exists()) {
          throw new AozanException(
              "Fastq file " + fastqFile.getName() + " doesn't exist");
        }

        FastqReader fastqReader = null;
        try {
          // Get compression value
          final CompressionType zType = getFastqSample().getCompressionType();

          // Append compressed fastq file to uncompressed file
          final InputStream is =
              zType.createInputStream(new FileInputStream(fastqFile));

          fastqReader = new FastqReader(is);
          IlluminaReadId ill = null;

          for (final ReadSequence seq : fastqReader) {

            if (ill == null) {
              ill = new IlluminaReadId(seq);
            } else {
              ill.parse(seq);
            }

            if (!ill.isFiltered()) {
              if (comptReadsPF % step == 0) {
                // Write in tmp fastq file
                fwTmpFastq.write(seq.toFastQ());
                fwTmpFastq.flush();

                if (--countReadsToCopyByFastq <= 0) {
                  break;
                }

              }
              comptReadsPF++;
            }
          }

          // Throw an exception if an error has occurred while reading data
          fastqReader.throwException();

        } catch (final IOException e) {
          throw new AozanException(e);
        } catch (final EoulsanException ee) {
          throw new AozanException(ee);
        } catch (final BadBioEntryException bbe) {
          throw new AozanException(bbe);

        } finally {

          if (fastqReader != null) {
            try {
              // Close fastqReader and inputStream on fastq file
              fastqReader.close();
            } catch (final IOException io) {
              LOGGER.warning(
                  "Exception occuring during the closing of FastqReader. Step collector "
                      + TemporaryPartialFastqCollector.COLLECTOR_NAME
                      + " for the sample "
                      + getFastqSample().getKeyFastqSample());
            }
          }
        }
      }

      if (fwTmpFastq != null) {
        fwTmpFastq.close();
      }

    } catch (final IOException io) {
      LOGGER.warning("Exception occuring during creating tmp file : "
          + this.tmpFastqFile.getAbsolutePath() + ". Step collector "
          + TemporaryPartialFastqCollector.COLLECTOR_NAME + " for the sample "
          + getFastqSample().getKeyFastqSample());

    } finally {
      if (fwTmpFastq != null) {
        try {
          fwTmpFastq.close();
        } catch (final IOException ignored) {
        }
      }

    }
  }

  /**
   * Write the temporary partial file from a array of fastq files, writing reads
   * randomly in files.
   * @throws AozanException if an error occurs while creating file
   */
  private void partialFastqFile() throws AozanException {

    Writer fwTmpFastq = null;

    try {
      fwTmpFastq =
          Files.newWriter(this.tmpFastqFile, Globals.DEFAULT_FILE_ENCODING);

      final int step =
          (int) (1 / ((double) this.countReadsPFtoCopy / this.rawClusterCount));

      int countReadsToCopyByFastq =
          this.countReadsPFtoCopy / getFastqSample().getFastqFiles().size();

      for (final File fastqFile : getFastqSample().getFastqFiles()) {
        int comptReadsPF = 1;

        if (!fastqFile.exists()) {
          throw new AozanException(
              "FastQ file " + fastqFile.getName() + " doesn't exist");
        }

        FastqReader fastqReader = null;
        try {
          // Get compression value
          final CompressionType zType = getFastqSample().getCompressionType();

          // Append compressed fastq file to uncompressed file
          final InputStream is =
              zType.createInputStream(new FileInputStream(fastqFile));

          fastqReader = new FastqReader(is);

          for (final ReadSequence seq : fastqReader) {

            if (comptReadsPF % step == 0) {
              // Write in tmp fastq file
              fwTmpFastq.write(seq.toFastQ());
              fwTmpFastq.flush();

              if (--countReadsToCopyByFastq <= 0) {
                break;
              }

            }
            comptReadsPF++;
          }

          // Throw an exception if an error has occurred while reading data
          fastqReader.throwException();

        } catch (final IOException e) {
          throw new AozanException(e);
        } catch (final BadBioEntryException bbe) {
          throw new AozanException(bbe);

        } finally {

          if (fastqReader != null) {
            try {
              fastqReader.close();
            } catch (final IOException io) {
              LOGGER.warning(
                  "Exception occurred during the closing of FastqReader. Step collector "
                      + TemporaryPartialFastqCollector.COLLECTOR_NAME
                      + " for the sample "
                      + getFastqSample().getKeyFastqSample());
            }
          }
        }
      }

    } catch (final IOException io) {
      LOGGER.warning("Exception occurred during creating tmp file : "
          + this.tmpFastqFile.getAbsolutePath() + ". Step collector "
          + TemporaryPartialFastqCollector.COLLECTOR_NAME + " for the sample "
          + getFastqSample().getKeyFastqSample());

    } finally {
      try {
        if (fwTmpFastq != null) {
          fwTmpFastq.close();
        }
      } catch (final IOException ignored) {
      }
    }
  }

  /**
   * Write the temporary partial file from a array of fastq files, all reads are
   * writing.
   * @throws AozanException if an error occurs while creating file
   */
  private void uncompressedFastqFile() throws AozanException {
    try {

      final OutputStream out = new FileOutputStream(this.tmpFastqFile);

      for (final File fastqFile : getFastqSample().getFastqFiles()) {

        if (!fastqFile.exists()) {
          throw new IOException(
              "FastQ file " + fastqFile.getName() + " doesn't exist");
        }

        // Get compression value
        final CompressionType zType = getFastqSample().getCompressionType();

        // Append compressed fastq file to uncompressed file
        final InputStream in = new FileInputStream(fastqFile);
        FileUtils.append(zType.createInputStream(in), out);
      }

      out.close();

    } catch (final IOException io) {
      throw new AozanException(io);
    }
  }

  @Override
  protected void createReportFile() throws AozanException, IOException {
    // No report
  }

  //
  // Constructor
  //

  /**
   * Thread constructor.
   * @param fastqSample fastq sample instance
   * @param rawClusterCount raw cluster count for the sample
   * @param pfClusterCount passing filter cluster count for the sample
   * @param numberReadsToCopy number reads in partial fastq to create
   * @param maxReadsToParse maximum number reads to parse for create partial
   *          fastq
   * @throws AozanException if an error occurs while creating sequence file for
   *           FastQC
   */
  public TemporaryPartialFastqThread(final FastqSample fastqSample,
      final int rawClusterCount, final int pfClusterCount,
      final int numberReadsToCopy, final int maxReadsToParse)
          throws AozanException {
    super(fastqSample);

    final int maxReadsPFtoParse = maxReadsToParse;
    this.countReadsPFtoCopy = numberReadsToCopy;

    this.rawClusterCount = rawClusterCount;
    this.pfClusterCountParsed =
        maxReadsPFtoParse > pfClusterCount ? pfClusterCount : maxReadsPFtoParse;

    this.tmpFastqFile =
        new File(getFastqStorage().getTemporaryFile(fastqSample) + ".tmp");
  }
}
