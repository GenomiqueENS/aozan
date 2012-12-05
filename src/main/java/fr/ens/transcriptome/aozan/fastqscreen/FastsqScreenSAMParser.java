package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.SAMParserLine;

public class FastsqScreenSAMParser implements SAMParserLine {

  // private final ReporterIncrementer incrementer;
  private static final Pattern PATTERN = Pattern.compile("\t");

  private File mapOutputFile = null;
  private String genome;
  private FileWriter fw;

  private final List<String> genomeDescriptionList = new ArrayList<String>();
  // private final String counterGroup;
  private final SAMParser parser;
  private boolean newGenome = true;

  private final List<ReadAlignmentsFilter> listFilters;
  private final ReadAlignmentsFilter filter;
  private final ReadAlignmentsFilterBuffer buffer;

  int count = 0;
  private int readsprocessed = 1;
  
  //TODO : add javadoc
  // add no test if read < 20, because all read are a size of 50 (cf rapport fastqc)
  
  
  // write in SAMOutputFile only mapping read
  // line : name read and genome referenced
  public void parseLine(String SAMline) throws IOException {
    //++count;
    //if (count % 100 == 0 )System.out.println(count+"  SAM line               " /*+ SAMline*/);
    
    if (SAMline == null || SAMline.length() == 0)
      return;

    if (SAMline.charAt(0) == '@') {
      // System.out.println("SAM line " + SAMline);
      genomeDescriptionList.add(SAMline);
      newGenome = true;
    } else {
      if (newGenome) {
        // Set the chromosomes sizes in the parser
        parser.setGenomeDescription(genomeDescriptionList);
        newGenome= false;
      }

      SAMRecord samRecord = parser.parseLine(SAMline);
      boolean result = buffer.addAlignment(samRecord);

      // new read
      if (!result) {
        List<SAMRecord> records = buffer.getFilteredAlignments();
        
        // count readsprocessed
        readsprocessed++;
        
       //if ((readsprocessed % 10000) == 0) System.out.println("en cours "+readsprocessed);
        
        if (records.size() > 0) {
          
          //if (records.size() > 1)
            // System.out.print("\n"
//                + result + "\t record " + samRecord.getReadName() + "\tsize : "
//                + records.size());
//          
          // define number of hits 1 or 2 (over one)
          int nbHits = records.size() == 1 ? 1 : 2;
          String nameRead = records.get(0).getReadName();

          // System.out.print("\t\tmapped"+result + "\t record "+ nameRead);

          // write in SAMmapOutputFile
          if (nameRead != null) {
            fw.write(nameRead + "\t" + nbHits + genome);
            fw.write("\n");
          }
        }
        buffer.addAlignment(samRecord);
        records.clear();
      } // if
    }
  }

  public void parserLine(File SAMFile) throws IOException {
    FileReader fr = new FileReader(SAMFile);
    BufferedReader br = new BufferedReader(fr);
    String line;

    while ((line = br.readLine()) != null) {
      parseLine(line);
    }

    br.close();
    fr.close();
  }

  // TODO to remove
  public void parserLine_OLD(String inputSAM) throws IOException {
    // if read mapped on the genome, add line in SAMOutputFile

    if (inputSAM == null || inputSAM.length() == 0 || inputSAM.charAt(0) == '@')
      return;

    String[] tokens = PATTERN.split(inputSAM, 3);
    String nameRead = null;

    // flag of SAM format are in case 2 and flag = 4 for read unmapped
    if (Integer.parseInt(tokens[1]) != 4)
      nameRead = tokens[0];

    // write in SAMmapOutputFile
    if (nameRead != null) {
      fw.write(nameRead + "\t" + genome);
      fw.write("\n");
    }
  }

  public File getSAMOutputFile() {
    return this.mapOutputFile;
  }

  public void cleanup() {
    // TODO to complete
    try {
      // processing read buffer - end of input stream bowtie execution
      List<SAMRecord> records = buffer.getFilteredAlignments();
      if (records.size() != 0) {
        String nameRead = records.get(0).getReadName();

        // write in SAMmapOutputFile
        if (nameRead != null) {
          fw.write(nameRead + "\t" + genome);
          fw.write("\n");
        }
      }
      fw.close();
     // System.out.println("\noutput File for genome " + genome + " - close");

    } catch (IOException ioe) {

    }
  }

  public void setup() {
  }

  //
  // GETTER
  //
  
  public int getReadsprocessed(){
    return this.readsprocessed;
  }
  //
  // CONSTRUCTOR
  //

  public FastsqScreenSAMParser(File MapOutputFile, String genome)
      throws IOException, EoulsanException {
    this.mapOutputFile = MapOutputFile;
    this.genome = genome;

    // Create parser object
    this.parser = new SAMParser();

    // object used for the Sam read alignments filter
    listFilters = Lists.newArrayList();
    listFilters.add(new RemoveUnmappedReadAlignmentsFilter());

    filter = new MultiReadAlignmentsFilter(listFilters);
    buffer = new ReadAlignmentsFilterBuffer(filter);

    fw = new FileWriter(this.mapOutputFile);

    // TODO add tests & checks
    // this.incrementer = new ReporterIncrementer();
    // this.counterGroup = counterGroup;
  }
}
