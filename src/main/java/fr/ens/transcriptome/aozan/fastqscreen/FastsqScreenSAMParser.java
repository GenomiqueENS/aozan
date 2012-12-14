/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.SAMParserLine;

/**
 * @author sperrin
 */
public class FastsqScreenSAMParser implements SAMParserLine {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private File mapOutputFile = null;
  private String genome;
  private FileWriter fw;

  private final List<String> genomeDescriptionList = new ArrayList<String>();
  private final SAMParser parser;
  private boolean newGenome = true;
  private boolean paired = false;

  private final List<ReadAlignmentsFilter> listFilters;
  private final ReadAlignmentsFilter filter;
  private final ReadAlignmentsFilterBuffer buffer;

  int count = 0;
  private int readsprocessed = 1;

  /**
   * call for each line of SAM file. Method create a new file, it contains a
   * line for each read mapped with her name and mapping data : first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome
   * @param SAMline parse SAM line
   * @throws IOException
   */
  public void parseLine(String SAMline) throws IOException {

    if (SAMline == null || SAMline.length() == 0)
      return;

    if (SAMline.charAt(0) == '@') {

      genomeDescriptionList.add(SAMline);
      newGenome = true;
    } else {
      if (newGenome) {
        // Set the chromosomes sizes in the parser
        parser.setGenomeDescription(genomeDescriptionList);
        newGenome = false;
      }

      SAMRecord samRecord = parser.parseLine(SAMline);
      boolean result = buffer.addAlignment(samRecord);

      // new read
      if (!result) {
        List<SAMRecord> records = buffer.getFilteredAlignments();

        // count readsprocessed
        readsprocessed++;

        if (records.size() > 0) {
          String nameRead = records.get(0).getReadName();

          // define number of hits 1 or 2 (over one)
          int nbHits;
          if (paired){
            // format output SAM from bowtie in mode paired, lost data from file
            // source R1 or R2
            // no distinct if one read mapped twice on genome or each read R1
            // and R2 mapped at the same position
            nbHits = ((records.size() / 2) == 1) ? 1 : 2;
          } else {
            nbHits = records.size() == 1 ? 1 : 2;
          }
          
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

  /**
   * parse a SAM file and create a new file, it contains a line for each read
   * mapped with her name and mapping data : first character represent the
   * number of hits for a read : 1 or 2 (for several hits) and the end represent
   * the name of reference genome
   * @param SAMFile parse SAM file create by bowtie
   * @throws IOException
   */
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

  public File getSAMOutputFile() {
    return this.mapOutputFile;
  }

  public void closeMapOutpoutFile() {
    // processing read buffer - end of input stream bowtie execution
    try {
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

    } catch (IOException io) {
      io.printStackTrace();
    }
  }

  @Override
  public void cleanup() {
  }

  @Override
  public void setup() {
  }

  //
  // GETTER
  //

  public int getReadsprocessed() {
    return this.readsprocessed;
  }

  //
  // CONSTRUCTOR
  //

  /**
   * initialize FastqScreenSAMParser : create the mapOutputFile and the list
   * filters used for parsing SAM file
   * @param MapOutputFile
   * @param genome
   * @throws IOException
   */
  public FastsqScreenSAMParser(File MapOutputFile, String genome, boolean paired)
      throws IOException {

    this.genome = genome;
    this.paired = paired;

    // Create parser object
    this.parser = new SAMParser();

    // object used for the Sam read alignments filter
    listFilters = Lists.newArrayList();
    listFilters.add(new RemoveUnmappedReadAlignmentsFilter());

    filter = new MultiReadAlignmentsFilter(listFilters);
    buffer = new ReadAlignmentsFilterBuffer(filter);

    this.mapOutputFile = MapOutputFile;
    fw = new FileWriter(this.mapOutputFile);

  }
}
