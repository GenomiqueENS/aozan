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

package fr.ens.transcriptome.aozan.collectors;

import java.io.IOException;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.io.FastqStorage;

/**
 * The abstract class define a thread, it calls by AbtractFastqCollector.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
abstract class AbstractFastqProcessThread implements Runnable {

  protected final FastqSample fastqSample;
  protected final RunData results;
  protected final FastqStorage fastqStorage;

  protected AozanException exception;
  protected boolean success;
  protected boolean dataSave;

  /**
   * Create a report file for the sample treated.
   * @throws AozanException
   * @throws IOException
   */
  abstract protected void createReportFile() throws AozanException, IOException;

  /**
   * Execute the treatment on a sample and supplying the rundata.
   * @throws AozanException
   */
  abstract protected void processResults() throws AozanException;

  /**
   * Get the results of the analysis.
   * @return a RunData object with only the result of the thread
   */
  public RunData getResults() {

    return this.results;
  }

  /**
   * Get the exception generated by the call to processSequences in the run()
   * method.
   * @return a exception object or null if no Exception has been thrown
   */
  public Exception getException() {

    return this.exception;
  }

  /**
   * Test if the call to run method was a success
   * @return true if the call to run method was a success
   */
  public boolean isSuccess() {

    return this.success;
  }

  /**
   * Test if the data file is saving
   * @return true if the data file is saving else false
   */
  public boolean isDataSave() {

    return this.dataSave;
  }

  /**
   * Set the data file is saving
   */
  public void setDataSave() {

    this.dataSave = true;
  }

  /**
   * Return the fastqSample which represent a sample to treat
   * @return fastqSample, object which represent a sample to treat
   */
  public FastqSample getFastqSample() {
    return this.fastqSample;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param fastqSample, object which represent a sample to treat
   * @throws AozanException if the fastqSample return none fastq file.
   */
  public AbstractFastqProcessThread(final FastqSample fastqSample)
      throws AozanException {

    // Check if fastqSample is null
    if (fastqSample == null)
      throw new AozanException("No fastqSample defined");

    this.fastqSample = fastqSample;

    // Check if fastq files exists for this fastqSample
    if (this.fastqSample.getFastqFiles() == null
        || this.fastqSample.getFastqFiles().isEmpty())
      throw new AozanException("No fastq file defined");

    this.results = new RunData();

    this.fastqStorage = FastqStorage.getInstance();

  }
}