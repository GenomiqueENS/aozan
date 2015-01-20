/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.demux;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.amazonaws.services.cloudfront.model.InvalidArgumentException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastqWriter;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVReader;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class allow to retrieve index from undetermined indices.
 * @author Laurent Jourdren
 */
public class ReDemux {

  private static final int INDEX_LENGTH = 6;

  private final File inputDir;
  private final File outputDir;
  private final CasavaDesign design;
  private final Map<Integer, ReDemuxLane> lanesToRedemux = Maps.newHashMap();

  /**
   * Redemux a lane.
   */
  private static class ReDemuxLane {

    private final CasavaDesign design;
    private final int lane;
    private final List<Integer> reads;
    private final File inputDir;
    private final File outputDir;
    private final Map<Pattern, CasavaSample> newIndexes = Maps.newHashMap();

    /**
     * Add an index for the re-demultiplexing.
     * @param index the index
     * @throws AozanException if the index is invalid
     */
    public void addIndex(final String index) throws AozanException {

      checkNotNull(index, "index cannot be null");

      final String upperIndex = index.trim().toUpperCase();

      checkArgument(upperIndex.length() == INDEX_LENGTH,
          "Invalid index, the length of the index must be "
              + INDEX_LENGTH + " : " + index);

      final char[] array = upperIndex.toCharArray();

      for (int i = 0; i < array.length; i++) {

        switch (array[i]) {

        case 'A':
        case 'T':
        case 'G':
        case 'C':
        case 'N':
        case '.':
          break;

        default:
          throw new InvalidArgumentException(
              "Invalid character found for index, 'A', 'T', 'G', 'C', 'N' and '.' are only allowed: "
                  + index);
        }
      }

      if (index.indexOf('.') != -1) {
        addIndexRegex(upperIndex);
      } else {
        addIndexSequence(upperIndex);
      }

    }

    /**
     * Add a regular expression index for the re-demultiplexing.
     * @param index the index
     * @throws AozanException if the index is invalid
     */
    private void addIndexRegex(final String index) throws AozanException {

      Preconditions.checkNotNull(index, "Index argument cannot be null");

      final Pattern pattern = Pattern.compile(index);
      CasavaSample sample = null;

      for (CasavaSample s : design.getSampleInLane(this.lane)) {

        if (pattern.matcher(s.getIndex()).matches()) {

          // Check if the index matches with more than one sample
          if (sample != null)
            throw new AozanException(
                "More than one sample matches with index on lane "
                    + this.lane + ": " + index);
          sample = s;
        }
      }

      // Check if the index matches with one sample
      if (sample == null) {
        throw new AozanException("No sample matches with index on lane "
            + this.lane + ": " + index);
      }
      this.newIndexes.put(pattern, sample);
    }

    /**
     * Add a standard index for the re-demultiplexing.
     * @param index the index
     * @throws AozanException if the index is invalid
     */
    private void addIndexSequence(final String index) throws AozanException {

      Preconditions.checkNotNull(index, "Index argument cannot be null");

      CasavaSample sample = null;
      int bestScore = Integer.MAX_VALUE;
      int bestCoreCount = 0;

      for (CasavaSample s : design.getSampleInLane(this.lane)) {

        final String sampleIndex = s.getIndex();

        final int mismatches = mismatches(index, sampleIndex);

        if (mismatches < bestScore) {

          bestScore = mismatches;
          bestCoreCount = 1;
          sample = s;
        } else if (mismatches == bestScore) {
          bestCoreCount++;
        }
      }

      // Check if the index matches with one sample
      if (sample == null) {
        throw new AozanException("No sample matches with index on lane "
            + this.lane + ": " + index);
      }

      // Check the number of mismatches of the best score
      if (bestScore > 2) {
        throw new AozanException("Cannot allow more than 2 mismatches on lane "
            + this.lane + ": " + index);
      }

      // Check if the index matches with more than one sample
      if (bestCoreCount > 1) {
        throw new AozanException(
            "More than one sample matches with index on lane "
                + this.lane + ": " + index);
      }

      this.newIndexes.put(Pattern.compile(index), sample);
    }

    /**
     * Re-demultiplex all the reads.
     * @throws IOException if an error occurs while re-demultiplexing
     * @throws BadBioEntryException if FASTQ entry read is invalid
     */
    private void reDemux() throws IOException, BadBioEntryException {

      if (this.reads.isEmpty()) {
        throw new EoulsanRuntimeException("No undetermined file found");
      }

      for (int i : this.reads) {

        if (i < 1) {
          throw new EoulsanRuntimeException(
              "The read for the undetermined file cannot be lower than 1 in lane "
                  + this.lane + ": " + i);
        }

        if (i > 2) {
          throw new EoulsanRuntimeException(
              "The read for the undetermined file cannot be greater than 2 in lane "
                  + this.lane + ": " + i);
        }

        // Launch demultiplexing for the lane
        reDemux(i);
      }
    }

    /**
     * Re-demultiplex a read.
     * @param read
     * @throws IOException
     * @throws BadBioEntryException
     */
    private void reDemux(int read) throws IOException, BadBioEntryException {

      // Check if directory exists
      if (!inputDir.isDirectory())
        return;

      // Get the list of files to process
      final List<File> undeterminedFiles =
          findUndeterminedFiles(this.inputDir, this.lane, read);

      if (undeterminedFiles.size() == 0) {
        throw new IOException("No undetermined file found");
      }

      // Get the compression of the undetermined file to use the same
      // compression for the new files
      final CompressionType compression =
          CompressionType.getCompressionTypeByFilename(undeterminedFiles.get(0)
              .getName());

      // Create the writers
      final Map<Pattern, FastqWriter> writers =
          createWriters(read, compression);

      // Create an array with the pattern. An array is faster than a collection.
      final Pattern[] patterns = writers.keySet().toArray(new Pattern[0]);

      for (File file : undeterminedFiles) {

        final FastqReader reader = new FastqReader(createInputStream(file));

        for (ReadSequence rs : reader) {

          // Do not use IlluminaReadId class because manual parsing is faster
          final String seqName = rs.getName();
          final String index = seqName.substring(seqName.lastIndexOf(':') + 1);

          for (Pattern p : patterns) {
            if (p.matcher(index).matches()) {
              writers.get(p).write(rs);
              // status.add(p);
              break;
            }
          }
        }

        reader.throwException();
        reader.close();

        // Close writers
        for (FastqWriter out : writers.values()) {
          out.close();
        }

      }

    }

    /**
     * Create an uncompressed input stream for an undetermined file.
     * @param file file to open
     * @return an InputStream object.
     * @throws IOException
     */
    private static InputStream createInputStream(final File file)
        throws IOException {

      final CompressionType compression =
          CompressionType.getCompressionTypeByFilename(file.getName());

      return compression.createInputStream(new FileInputStream(file));
    }

    /**
     * Create writers that compress output if needed.
     * @param read read to re-demultiplex
     * @param compression compression of the writers
     * @return a map with the writers
     * @throws FileNotFoundException if one of the output files cannot be
     *           created
     * @throws IOException if one of the output files cannot be created
     */
    private Map<Pattern, FastqWriter> createWriters(final int read,
        final CompressionType compression) throws FileNotFoundException,
        IOException {

      final Map<Pattern, FastqWriter> result = Maps.newHashMap();

      for (Map.Entry<Pattern, CasavaSample> e : this.newIndexes.entrySet()) {

        final String sampleProject = e.getValue().getSampleProject();
        final String sampleName = e.getValue().getSampleId();
        final String sampleIndex = e.getValue().getIndex();

        // Define the output directory
        final File subdir =
            new File(this.outputDir, "Project_"
                + sampleProject + File.separator + "Sample_" + sampleName);

        // Create output directory if not exists
        if (!subdir.isDirectory()) {

          if (!subdir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + subdir);
          }
        }

        // Define the output file
        final File file =
            new File(subdir, sampleName
                + "_" + sampleIndex + "_L00" + lane + lane + "_R" + read
                + "_redemux.fastq" + compression.getExtension());

        final OutputStream out =
            compression.createOutputStream(new FileOutputStream(file));

        result.put(e.getKey(), new FastqWriter(out));
      }

      return result;
    }

    /**
     * Find the undetermined files.
     * @param baseDir input directory
     * @param lane lane of of the undetermined files
     * @param read read to to process
     * @return an array with the undetermined files
     */
    private static List<File> findUndeterminedFiles(final File baseDir,
        final int lane, final int read) {

      return Arrays.asList(baseDir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File arg0) {

          return arg0.getName().startsWith(
              "lane" + lane + "_Undetermined_L00" + lane + "_R" + read + "_");
        }
      }));
    }

    /**
     * Find the read numbers of the undetermined files of the lane
     * @param baseDir the directory of the undetermined files
     * @param lane the lane
     * @return a list of of the read numbers
     */
    private static List<Integer> findReads(final File baseDir, final int lane) {

      List<File> undeterminedFiles =
          Arrays.asList(baseDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File arg0) {

              return arg0.getName().startsWith(
                  "lane" + lane + "_Undetermined_L00" + lane + "_R");
            }
          }));

      final List<Integer> result = new ArrayList<>();

      // Find read number in the filenames
      for (File f : undeterminedFiles) {
        final String filename = f.getName();
        final String prefix = filename.substring(0, filename.lastIndexOf('_'));
        result.add(Integer.parseInt(prefix.substring(prefix.length() - 1)));
      }

      Collections.sort(result);

      return result;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param design Casava design object
     * @param lane lane to re-demultiplex
     * @param inputDir input directory
     * @param outputDir output directory
     */
    public ReDemuxLane(final CasavaDesign design, final int lane,
        final File inputDir, final File outputDir) {

      this.design = design;
      this.lane = lane;
      this.inputDir =
          new File(inputDir, "/Undetermined_indices/Sample_lane" + lane);
      this.outputDir = outputDir;
      this.reads = findReads(this.inputDir, lane);
    }

  }

  /**
   * Add an index to re-demultiplex.
   * @param lane the lane to re-demultiplex
   * @param index the index
   * @throws AozanException if the index is invalid
   */
  public void addIndex(final int lane, final String index)
      throws AozanException {

    Preconditions.checkArgument(lane >= 1 && lane <= 8, "Invalid lane number: "
        + lane);

    final ReDemuxLane reDemuxLane;

    if (!this.lanesToRedemux.containsKey(lane)) {
      reDemuxLane = new ReDemuxLane(design, lane, inputDir, this.outputDir);
      this.lanesToRedemux.put(lane, reDemuxLane);
    } else
      reDemuxLane = this.lanesToRedemux.get(lane);

    reDemuxLane.addIndex(index);
  }

  /**
   * Launch the re-demultiplexing.
   * @throws IOException if an IO error occurs while re-demultiplexing
   * @throws BadBioEntryException if an FASTQ entry read is invald
   * @throws EoulsanException
   */
  public void redmux() throws IOException, BadBioEntryException {

    for (ReDemuxLane rdl : this.lanesToRedemux.values())
      rdl.reDemux();
  }

  /**
   * Get the number of mismatches of two string of the same length.
   * @param a the first string
   * @param b the second string
   * @return the number of mismatches
   */
  private static final int mismatches(final String a, final String b) {

    Preconditions.checkNotNull(a, "a cannot be null");
    Preconditions.checkNotNull(b, "b cannot be null");
    Preconditions.checkArgument(a.length() == b.length(),
        "The length of the 2 String must be equals");

    final int len = a.length();
    int result = 0;

    for (int i = 0; i < len; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        result++;
      }
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param baseDir run base directory
   * @param design Casava design object
   * @param outputDir output directory
   */
  public ReDemux(final File baseDir, final CasavaDesign design,
      final File outputDir) {

    Preconditions.checkNotNull(design, "design argument cannot be null");
    Preconditions.checkNotNull(baseDir, "baseDir argument cannot be null");
    this.design = design;
    this.inputDir = baseDir;
    this.outputDir = outputDir;
  }

  //
  // Static methods
  //

  public static void redemultiplex(final File designFile,
      final List<String> lanesAndIndex, final File outputDir)
      throws FileNotFoundException, IOException, AozanException,
      BadBioEntryException {

    Preconditions.checkNotNull(designFile, "design file cannot be null");
    Preconditions.checkNotNull(lanesAndIndex, "laneAndIndex cannot be null");
    Preconditions.checkNotNull(outputDir, "output directory cannot be null");

    // Load design
    final CasavaDesign design = new CasavaDesignCSVReader(designFile).read();

    // Create ReDemux object
    ReDemux rd = new ReDemux(designFile.getParentFile(), design, outputDir);

    for (String s : lanesAndIndex) {

      String[] fields = s.split(":");

      if (fields.length != 2) {
        throw new AozanException("Invalid index entry: " + s);
      }

      try {
        rd.addIndex(Integer.parseInt(fields[0]), fields[1]);
      } catch (NumberFormatException e) {
        throw new AozanException("Invalid index entry: " + s);
      }
    }

    rd.redmux();
  }

  //
  // Main method
  //

  public static void main(final String[] args) throws FileNotFoundException,
      IOException, AozanException, BadBioEntryException, EoulsanException {

    final List<String> list = Arrays.asList(args);

    if (list.size() < 2) {
      System.err.println("Syntax: redmux rundir laneindex...");
      System.exit(1);
    }

    redemultiplex(new File(list.get(0)), list.subList(1, list.size()),
        new File(System.getProperty("user.dir")));
  }

}
