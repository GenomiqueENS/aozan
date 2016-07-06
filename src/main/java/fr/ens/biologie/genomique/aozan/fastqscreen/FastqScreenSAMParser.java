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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.fastqscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

import com.google.common.io.Files;

import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.SAMUtils;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.RemoveUnmappedReadAlignmentsFilter;

/**
 * This class ensures alignment fastqScreen treating the output format BAM of
 * mapper.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenSAMParser {

  // private File mapOutputFile = null;
  private final String genome;
  private final Writer fw;

  private final SAMLineParser parser;
  private boolean headerParsed = false;
  private final boolean pairedMode;

  private final ReadAlignmentsFilterBuffer buffer;

  private int readsprocessed = 0;

  /**
   * Parse a SAM file and create a new file mapoutsamfile, it contains a line
   * for each read mapped with her name and mapping data : first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome.
   * @param is inputStream to parse
   * @throws IOException
   */
  public void parseLines(final InputStream is) throws IOException {

    final BufferedReader br = new BufferedReader(
        new InputStreamReader(is, StandardCharsets.ISO_8859_1));

    String line = null;

    while ((line = br.readLine()) != null) {
      parseLine(line);
    }

    br.close();

    closeMapOutputFile();
  }

  /**
   * Call for each line of SAM file. Method create a new file, it contains a
   * line for each read mapped with her name and mapping data : first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome.
   * @param SAMline parse SAM line
   * @throws IOException if an error occurs while writing in mapOutputFile
   */
  private void parseLine(final String SAMline) throws IOException {

    if (SAMline == null || SAMline.length() == 0) {
      return;
    }

    if (!this.headerParsed) {

      if (SAMline.charAt(0) == '@') {
        return;

      } else {
        // Set the chromosomes sizes in the parser
        this.headerParsed = true;
      }
    }

    final SAMRecord samRecord = this.parser.parseLine(SAMline);
    final boolean result = this.buffer.addAlignment(samRecord);

    // Add a new read
    if (!result) {
      parseBuffered();
      this.buffer.addAlignment(samRecord);
    }
  }

  /**
   * Write last record and close file mapOutputFile.
   */
  void closeMapOutputFile() throws IOException {

    // processing read buffer - end of input stream bowtie execution
    if (this.headerParsed) {
      parseBuffered();
    }

    this.fw.close();
  }

  private void parseBuffered() throws IOException {

    final List<SAMRecord> records = this.buffer.getFilteredAlignments();

    if (records != null && records.size() > 0) {
      final String nameRead = records.get(0).getReadName();

      int nbHits;
      // mode paired : records contains an event number of reads
      if (this.pairedMode) {
        nbHits = records.size() == 2 ? 1 : 2;
      } else {
        nbHits = records.size() == 1 ? 1 : 2;
      }

      // write in SAMmapOutputFile
      if (nameRead != null) {
        this.fw.write(nameRead + "\t" + nbHits + this.genome);
        this.fw.write("\n");
      }
    }

    if (records != null) {
      records.clear();
    }

    this.readsprocessed++;
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
   * filters used for parsing SAM file.
   * @param mapOutputFile file result from mapping
   * @param genome name genome
   * @param genomeDescription description of the genome
   * @param pairedMode true if a pair-end run and option paired mode equals true
   *          else false
   * @throws IOException if an error occurs while initializing mapOutputFile
   */
  public FastqScreenSAMParser(final File mapOutputFile, final String genome,
      final boolean pairedMode, final GenomeDescription genomeDescription)
      throws IOException {

    this.genome = genome;
    this.pairedMode = pairedMode;

    // Create parser object
    this.parser =
        new SAMLineParser(SAMUtils.newSAMFileHeader(genomeDescription));

    // object used for the Sam read alignments filter
    final List<ReadAlignmentsFilter> listFilters = new ArrayList<>();
    listFilters.add(new RemoveUnmappedReadAlignmentsFilter());

    final ReadAlignmentsFilter filter =
        new MultiReadAlignmentsFilter(listFilters);
    this.buffer = new ReadAlignmentsFilterBuffer(filter);

    this.fw = Files.newWriter(mapOutputFile, Globals.DEFAULT_FILE_ENCODING);
  }
}
