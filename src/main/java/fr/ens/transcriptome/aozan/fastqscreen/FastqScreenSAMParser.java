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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.eoulsan.bio.SAMParserLine;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter;

/**
 * This class ensures alignment fastqScreen treating the output format BAM of
 * mapper.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenSAMParser implements SAMParserLine {

  private File mapOutputFile = null;
  private String genome;
  private Writer fw;

  private final List<String> genomeDescriptionList;
  private final SAMParser parser;
  private boolean headerParsed = false;
  private boolean pairedMode = false;

  private final List<ReadAlignmentsFilter> listFilters;
  private final ReadAlignmentsFilter filter;
  private final ReadAlignmentsFilterBuffer buffer;

  private int readsprocessed = 0;

  /**
   * Call for each line of SAM file. Method create a new file, it contains a
   * line for each read mapped with her name and mapping data : first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome
   * @param SAMline parse SAM line
   * @throws IOException if an error occurs while writing in mapOutputFile
   */
  @Override
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

      if (records != null && records.size() > 0) {

        String nameRead = records.get(0).getReadName();

        int nbHits;

        // define number of hits 1 or 2 (over one)
        if (pairedMode)
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

      if (records != null)
        records.clear();
    }

  }

  /**
   * Parse a SAM file and create a new file mapoutsamfile, it contains a line
   * for each read mapped with her name and mapping data : first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome
   * @param is inputStream to parse
   * @return number lines read
   * @throws IOException
   */
  public long parseLine(final InputStream is) throws IOException {

    BufferedReader br =
        new BufferedReader(new InputStreamReader(is, Charsets.ISO_8859_1));
    String line = null;
    long compt = 0;
    while ((line = br.readLine()) != null) {
      parseLine(line);
      compt++;
    }

    br.close();
    is.close();

    return compt;
  }

  /**
   * Parse a SAM file and create a new file, it contains a line for each read
   * mapped with her name and mapping data : first character represent the
   * number of hits for a read : 1 or 2 (for several hits) and the end represent
   * the name of reference genome
   * @param SAMFile parse SAM file create by bowtie
   * @throws IOException
   */
  public void parseLine(final File SAMFile) throws IOException {

    BufferedReader br = Files.newReader(SAMFile, Charsets.ISO_8859_1);
    String line;

    while ((line = br.readLine()) != null) {
      parseLine(line);
    }
    br.close();
  }

  public File getSAMOutputFile() {
    return this.mapOutputFile;
  }

  /**
   * Write last record and close file mapOutputFile
   */
  public void closeMapOutputFile() throws IOException {
    // processing read buffer - end of input stream bowtie execution
    readsprocessed++;
    List<SAMRecord> records = buffer.getFilteredAlignments();

    if (records != null && records.size() > 0) {
      String nameRead = records.get(0).getReadName();

      int nbHits;
      // mode paired : records contains an event number of reads
      if (pairedMode)
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

  }

  @Override
  public void cleanup() {
  }

  @Override
  public void setup() {
  }

  //
  // Getters
  //

  public int getReadsprocessed() {

    return this.readsprocessed;
  }

  //
  // Constructor
  //

  /**
   * Initialize FastqScreenSAMParser : create the mapOutputFile and the list
   * filters used for parsing SAM file
   * @param mapOutputFile file result from mapping
   * @param genome name genome
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @throws IOException if an error occurs while initializing mapOutputFile
   */
  public FastqScreenSAMParser(final File mapOutputFile, final String genome,
      final boolean pairedMode) throws IOException {

    this.genome = genome;
    this.pairedMode = pairedMode;

    // Create parser object
    this.parser = new SAMParser();

    // object used for the Sam read alignments filter
    this.listFilters = Lists.newArrayList();
    this.listFilters.add(new RemoveUnmappedReadAlignmentsFilter());

    this.filter = new MultiReadAlignmentsFilter(listFilters);
    this.buffer = new ReadAlignmentsFilterBuffer(filter);

    this.genomeDescriptionList = new ArrayList<String>();

    this.mapOutputFile = mapOutputFile;
    this.fw =
        Files.newWriter(this.mapOutputFile, Globals.DEFAULT_FILE_ENCODING);
  }
}
