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
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import uk.ac.bbsrc.babraham.FastQC.Sequence.Sequence;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;

public class SequenceFileAozan implements SequenceFile {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /** Timer **/
  private Stopwatch timer;

  private final File tmpFile;
  private final SequenceFile seqFile;
  private final FastqSample fastqSample;
  private final FileWriter fw;

  @Override
  public Sequence next() throws SequenceFormatException {

    Sequence seq = null;

    // Writing in temporary file
    try {
      seq = this.seqFile.next();

      this.fw.write(seq.getID() + "\n");
      this.fw.write(seq.getSequence() + "\n");

      if (seq.getColorspace() == null)
        this.fw.write("+\n");
      else
        this.fw.write(seq.getColorspace() + "\n");

      this.fw.write(seq.getQualityString() + "\n");

      // End of file, close the new file
      if (!seqFile.hasNext()) {
        this.fw.close();

        long sizeFile = tmpFile.length();

        // Rename file for remove '.tmp' final
        tmpFile.renameTo(new File(FastqStorage.getInstance().getTemporaryFile(
            fastqSample)));

        synchronized (this) {
          LOGGER.fine("In fastqc : end uncompress "
              + fastqSample.getFastqFiles().size()
              + " fastq file(s) for "
              + fastqSample.getName()
              + " in type compression "
              + fastqSample.getCompressionType()
              + " in "
              + toTimeHumanReadable(timer.elapsedMillis())
              + " : temporary fastq file size "
              + Globals.FORMATTER_MILLIER.format(sizeFile)
              + " (estimated size("
              + Globals.FORMATTER_MILLIER.format(fastqSample
                  .getUncompressedSize()) + "))");
        }
        timer.stop();

      }

    } catch (IOException io) {
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

  public SequenceFileAozan(final File[] files, final File tmpFile,
      final FastqSample fastqSample) throws AozanException {

    // Init timer
    this.timer = new Stopwatch().start();

    this.tmpFile = tmpFile;
    this.fastqSample = fastqSample;

    try {
      this.fw = new FileWriter(this.tmpFile);
      this.seqFile = SequenceFactory.getSequenceFile(files);

    } catch (SequenceFormatException e) {
      throw new AozanException(e.getMessage());

    } catch (IOException io) {
      throw new AozanException(io.getMessage());
    }
  }

}
