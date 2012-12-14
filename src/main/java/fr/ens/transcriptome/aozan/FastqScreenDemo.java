/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.Settings;

public class FastqScreenDemo {
  
  public static final Map<String, String> properties = Maps.newLinkedHashMap();
  public static final String RESOURCE_ROOT = "/home/sperrin/Documents/FastqScreenTest/resources";
  // public static final String RACINE = "/home/sperrin/shares-net/ressources/sequencages";
  public static final String SRC_RUN = "/home/sperrin/Documents/FastqScreenTest/runtest";
  //public static final String SRC_RUN = "/home/sperrin/shares-net/sequencages/runs";
  public static final String TMP_DIR = "/home/sperrin/Documents/FastqScreenTest/tmp";
  
  public static boolean paired = false;
  
  public static final void main(String[] args) throws AozanException,
      IOException, EoulsanException {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
    Settings settings = EoulsanRuntime.getSettings();

    settings
        .setGenomeDescStoragePath(RESOURCE_ROOT+"/genomes_descs");
    settings
        .setGenomeMapperIndexStoragePath(RESOURCE_ROOT+"/mappers_indexes");
    settings
        .setGenomeStoragePath(RESOURCE_ROOT+"/genomes");
    Locale.setDefault(Locale.US);

    
    
    String runId;
    if (paired) {
      // run test pair-end
      runId = "120830_SNL110_0055_AD16D9ACXX";
    } else {
      //run test single-end
      runId = "121116_SNL110_0058_AC11HRACXX";
    }
    
    // include in RunDataGenerator
    final String fastqDir = SRC_RUN + "/qc_" + runId+"/"+runId;
    final String indexDir = RESOURCE_ROOT + "/genomes";
    
    // Sample tests
    properties.put("qc.fastqscreen.genomes", "phix,lsuref_dna,adapters2");
    properties.put("qc.fastqscreen.fastqDir", fastqDir);
    properties.put("qc.fastqscreen.indexDir", indexDir);
    properties.put("tmp.dir", TMP_DIR);
    // number threads used for fastqscreen is define in aozan.conf
    properties.put("qc.conf.fastqscreen.threads","4");

    // process for one run
    FastqScreenCollector fsqCollector = new FastqScreenCollector();

    RunData data = null;

    File f = new File(fastqDir + "/data-" + runId + ".txt");

    try {
      data = new RunData(f);
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
    
    // Configure : create list of reference genome
    fsqCollector.configure(properties);
    
    // And collect data
    fsqCollector.collect(data);

    // TODO test method
    // print completed rundata with results of fastqscreen
    try {
      FileWriter fw =
          new FileWriter(new File(SRC_RUN + "/RunDataCompleted.txt"));
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(data.toString());
      bw.close();
      fw.close();
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
  }
  
  
  /**
   * 
   */
  public static void printProperties(){
    for (Map.Entry<String, String> e : properties.entrySet()){
      System.out.println("key "+e.getKey()+"  value "+e.getValue());
    }
  }

}
