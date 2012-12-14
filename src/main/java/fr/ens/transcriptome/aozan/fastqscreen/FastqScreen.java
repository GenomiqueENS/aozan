/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  protected static final String COUNTER_GROUP = "fastqscreen";
  private static final String KEY_TMP_DIR = "tmp.dir";

  private Map<String, String> properties;
  private final static long startTime = System.currentTimeMillis();

  /**
   * mode pair-end : execute fastqscreen calcul
   * @param fastqRead1 fastq file input for mapper
   * @param fastqRead2 fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(File fastqRead,
      List<String> listGenomes) throws AozanException {
   
    return this.execute(fastqRead, null, listGenomes);
   
  }

  /**
   * mode single-end : execute fastqscreen calcul
   * @param fastqFile fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(File fastqRead1, File fastqRead2, List<String> listGenomes)
      throws AozanException {

    String tmpDir = properties.get(KEY_TMP_DIR);
    FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce();
    pmr.setMapReduceTemporaryDirectory(new File(tmpDir));

    try {

      if (fastqRead2 == null)
        pmr.doMap(fastqRead1, listGenomes, properties);
      else 
        pmr.doMap(fastqRead1, fastqRead2, listGenomes, properties);
        
      pmr.doReduce(new File(tmpDir + "/outputDoReduce.txt"));

    } catch (IOException e) {
      e.printStackTrace();
      throw new AozanException(e.getMessage());

    } catch (BadBioEntryException bad) {
      bad.printStackTrace();
      throw new AozanException(bad.getMessage());

    }

    final long endTime = System.currentTimeMillis();

    System.out.println((endTime - startTime)
        + " -- " + new SimpleDateFormat("h:m a").format(new Date()));

    return pmr.getFastqScreenResult();
  }

  //
  // CONSTRUCTOR
  //

  /**
   * @param properties properties defines in configuration of aozan
   */
  public FastqScreen(Map<String, String> properties) {
    this.properties = properties;
  }

}