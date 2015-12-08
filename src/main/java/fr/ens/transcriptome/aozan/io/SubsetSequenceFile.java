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

package fr.ens.transcriptome.aozan.io;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;

/**
 * The class allow to browse a fastq file sequence per sequence and writing them
 * in a new file.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class SubsetSequenceFile implements SequenceFile {
  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /** Timer. **/
  private Stopwatch timer;

  private final File tmpFile;
  private final File tmpFileWithExtension;
  private final SequenceFile seqFile;
  private final FastqSample fastqSample;
  private final Writer fw;

  /**
   * Go to the next sequence of fastq file and write this sequence in temporary
   * file.
   * @return Sequence the next sequence from a fastq file or null at the end
   * @throws SequenceFormatException it occurs if the format of sequence is
   *           different of fastq format .
   */
  @Override
  public Sequence next() throws SequenceFormatException {

    Sequence seq = null;

    // Writing in temporary file
    try {
      seq = this.seqFile.next();

      this.fw.write(seq.getID() + "\n");
      this.fw.write(seq.getSequence() + "\n");

      if (seq.getColorspace() == null) {
        this.fw.write("+\n");
      } else {
        this.fw.write(seq.getColorspace() + "\n");
      }

      this.fw.write(seq.getQualityString() + "\n");

      // End of file, close the new file
      if (!this.seqFile.hasNext()) {
        this.fw.close();

        long sizeFile = this.tmpFileWithExtension.length();
        sizeFile /= (1024 * 1024 * 1024);

        // Rename file for remove '.tmp' final
        if (!this.tmpFileWithExtension.renameTo(this.tmpFile)) {
          LOGGER.warning("Aozan sequence : fail to rename file "
              + this.tmpFile.getAbsolutePath());
        }

        this.timer.stop();

        LOGGER.fine("FASTQC: uncompress for "
            + this.fastqSample.getKeyFastqSample() + " "
            + +this.fastqSample.getFastqFiles().size()
            + " fastq file(s), type compression "
            + this.fastqSample.getCompressionType() + " in "
            + toTimeHumanReadable(this.timer.elapsed(TimeUnit.MILLISECONDS))
            + "(tmp fastq file size " + sizeFile + "Go / estimated size "
            + this.fastqSample.getUncompressedSize() + ")");
      }

    } catch (final IOException io) {
      throw new SequenceFormatException(io.getMessage());
    }

    return seq;
  }

  @Override
  public File getFile() {
    return this.seqFile.getFile();
  }

  @Override
  public int getPercentComplete() {
    return this.seqFile.getPercentComplete();
  }

  @Override
  public boolean hasNext() {
    return this.seqFile.hasNext();
  }

  @Override
  public boolean isColorspace() {
    return this.seqFile.isColorspace();
  }

  @Override
  public String name() {
    return this.seqFile.name();
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new aozan sequence file.
   * @param files the files
   * @param tmpFile the tmp file
   * @param fastqSample the fastq sample
   * @throws AozanException the aozan exception
   */
  public SubsetSequenceFile(final File[] files, final File tmpFile,
      final FastqSample fastqSample) throws AozanException {

    this.tmpFileWithExtension = new File(tmpFile + ".tmp");
    this.tmpFile = tmpFile;
    this.fastqSample = fastqSample;

    try {
      this.fw = Files.newWriter(this.tmpFileWithExtension,
          Globals.DEFAULT_FILE_ENCODING);

      this.seqFile = SequenceFactory.getSequenceFile(files);

      // Init timer
      this.timer = Stopwatch.createStarted();

    } catch (final SequenceFormatException e) {
      throw new AozanException(e);

    } catch (final IOException io) {
      throw new AozanException(io);
    }
  }

}
