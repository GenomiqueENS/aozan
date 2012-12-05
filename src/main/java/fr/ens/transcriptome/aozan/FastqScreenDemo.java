/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.FastQScreenCollector;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;

public class FastqScreenDemo {

  public static final void main(String[] args) throws AozanException,
      IOException, EoulsanException {
    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
    Locale.setDefault(Locale.US);

    // inlude in RunDataGenerator
    final String fastqDir = "/home/sperrin/Documents/FastqScreenTest/runtest58";
    final String bclDir = "/home/jourdren/shares-net/sequencages/bcl";
    final String qcDir = "/tmp";
    final String indexDir =
        "/home/sperrin/shares-net/ressources/sequencages/fastq_screen";

    final Map<String, String> properties = Maps.newLinkedHashMap();

    // Sample tests
    properties.put("qc.fastqscreen.PhiX.enable", "true");
    properties.put("qc.fastqscreen.Adapters.enable", "true");
//    properties.put("qc.fastqscreen.Silva_ribosomes/LSURef.enable", "true");

    // process for one run

    FastQScreenCollector fsqCollector = new FastQScreenCollector();

    RunData data = null;
    String runId = "121116_SNL110_0058_AC11HRACXX";
    
    File f = new File(fastqDir + "/data-"+runId+".txt");
    
    try {
      data = new RunData(f);
    } catch (IOException io) {
      io.getMessage();
    }

    // Configure : create list of reference genome
    fsqCollector.configure(properties, indexDir);

    // And collect data
    fsqCollector.collect(data);
  }

}
