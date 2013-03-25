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

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.bio.SAMParserLine;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter;

/**
 * This class ensures alignment fastqScreen treating the output format BAM of
 * mapper.
 * @author Sandrine Perrin
 */
public class FastsqScreenSAMParser implements SAMParserLine {

  private File mapOutputFile = null;
  private String genome;
  private FileWriter fw;

  private final List<String> genomeDescriptionList;
  private final SAMParser parser;
  private boolean headerParsed = false;
  private boolean paired = false;

  private final List<ReadAlignmentsFilter> listFilters;
  private final ReadAlignmentsFilter filter;
  private final ReadAlignmentsFilterBuffer buffer;

  private int readsprocessed = 0;
  private int readsmapped = 0;

  @Override
  /**
   * Call for each line of SAM file. Method create a new file, it contains a
   * line for each read mapped with her name and mapping data : first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome
   * @param SAMline parse SAM line
   * @throws IOException
   */
  public void parseLine(final String SAMline) throws IOException {

    if (SAMline == null || SAMline.length() == 0)
      return;

    if (!headerParsed) {

      if (SAMline.charAt(0) == '@') {

        // Store header line
        genomeDescriptionList.add(SAMline);

        return;

      } else {

        // Set the chromosomes sizes in the parser
        parser.setGenomeDescription(genomeDescriptionList);
        genomeDescriptionList.clear();
        headerParsed = true;

      }

    }

    final SAMRecord samRecord = parser.parseLine(SAMline);
    boolean result = buffer.addAlignment(samRecord);
    // new read
    if (!result) {
      readsprocessed++;

      List<SAMRecord> records = buffer.getFilteredAlignments();

      if (records.size() > 0) {

        String nameRead = records.get(0).getReadName();

        int nbHits;

        // define number of hits 1 or 2 (over one)
        if (paired)
          // mode paired : records contains an event number of reads
          nbHits = records.size() == 2 ? 1 : 2;
        else
          nbHits = records.size() == 1 ? 1 : 2;

        // write in SAMmapOutputFile
        if (nameRead != null) {
          fw.write(nameRead + "\t" + nbHits + genome);
          fw.write("\n");
        }
      }
      buffer.addAlignment(samRecord);
      records.clear();
    }

  }

  /**
   * Parse a SAM file and create a new file, it contains a line for each read
   * mapped with her name and mapping data : first character represent the
   * number of hits for a read : 1 or 2 (for several hits) and the end represent
   * the name of reference genome
   * @param SAMFile parse SAM file create by bowtie
   * @throws IOException
   */
  public void parserLine(final File SAMFile) throws IOException {
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

  /**
   * Write last record and close file mapOutputFile
   */
  public void closeMapOutputFile() {
    // processing read buffer - end of input stream bowtie execution
    try {
      readsprocessed++;
      List<SAMRecord> records = buffer.getFilteredAlignments();
      if (records.size() != 0) {
        String nameRead = records.get(0).getReadName();

        int nbHits;
        // mode paired : records contains an event number of reads
        if (paired)
          nbHits = records.size() == 2 ? 1 : 2;
        else
          nbHits = records.size() == 1 ? 1 : 2;

        // write in SAMmapOutputFile
        if (nameRead != null) {
          fw.write(nameRead + "\t" + nbHits + genome);
          fw.write("\n");
        }
      }
      fw.close();

    } catch (IOException io) {
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

  public int getReadsMapped() {
    return this.readsmapped;
  }

  //
  // CONSTRUCTOR
  //

  /**
   * Initialize FastqScreenSAMParser : create the mapOutputFile and the list
   * filters used for parsing SAM file
   * @param mapOutputFile file result from mapping
   * @param genome name genome
   * @throws IOException if an error occurs while initializing mapOutputFile
   */
  public FastsqScreenSAMParser(final File mapOutputFile, final String genome,
      final boolean paired) throws IOException {

    this.genome = genome;
    this.paired = paired;

    // Create parser object
    this.parser = new SAMParser();

    // object used for the Sam read alignments filter
    this.listFilters = Lists.newArrayList();
    this.listFilters.add(new RemoveUnmappedReadAlignmentsFilter());

    this.filter = new MultiReadAlignmentsFilter(listFilters);
    this.buffer = new ReadAlignmentsFilterBuffer(filter); // , true);

    this.genomeDescriptionList = new ArrayList<String>();

    this.mapOutputFile = mapOutputFile;
    this.fw = new FileWriter(this.mapOutputFile);

  }
}
