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

package fr.ens.biologie.genomique.aozan.demux;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.bio.io.FastqReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.FastqWriter;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;

/**
 * This class allow to retrieve index from undetermined indices.
 * @author Laurent Jourdren
 */
public class ReDemux {

  private static final int INDEX_LENGTH = 6;

  private final File inputDir;
  private final File outputDir;
  private final SampleSheet sampleSheet;
  private final Map<Integer, ReDemuxLane> lanesToRedemux = new HashMap<>();

  @SuppressWarnings("unused")
  private String bcl2fastqVersion;

  /**
   * Redemux a lane.
   */
  private static class ReDemuxLane {

    private final SampleSheet samplesheet;
    private final int lane;
    private final List<Integer> reads;
    private final File inputDir;
    private final File outputDir;
    private final Map<Pattern, Sample> newIndexes = new HashMap<>();

    /**
     * Add an index for the re-demultiplexing.
     * @param index the index
     * @throws AozanException if the index is invalid
     */
    public void addIndex(final String index) throws AozanException {

      requireNonNull(index, "index cannot be null");

      final String upperIndex = index.trim().toUpperCase();

      checkArgument(upperIndex.length() == INDEX_LENGTH,
          "Invalid index, the length of the index must be "
              + INDEX_LENGTH + " : " + index);

      final char[] array = upperIndex.toCharArray();

      for (char anArray : array) {

        switch (anArray) {

        case 'A':
        case 'T':
        case 'G':
        case 'C':
        case 'N':
        case '.':
          break;

        default:
          throw new IllegalArgumentException(
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

      requireNonNull(index, "Index argument cannot be null");

      final Pattern pattern = Pattern.compile(index);
      Sample sample = null;

      for (Sample s : samplesheet.getSampleInLane(this.lane)) {

        if (pattern.matcher(s.getIndex1()).matches()) {

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
        throw new AozanException(
            "No sample matches with index on lane " + this.lane + ": " + index);
      }
      this.newIndexes.put(pattern, sample);
    }

    /**
     * Add a standard index for the re-demultiplexing.
     * @param index the index
     * @throws AozanException if the index is invalid
     */
    private void addIndexSequence(final String index) throws AozanException {

      requireNonNull(index, "Index argument cannot be null");

      Sample sample = null;
      int bestScore = Integer.MAX_VALUE;
      int bestCoreCount = 0;

      for (Sample s : samplesheet.getSampleInLane(this.lane)) {

        final String sampleIndex = s.getIndex1();

        // TODO instead call mismatches method from Undetermined Thread
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
        throw new AozanException(
            "No sample matches with index on lane " + this.lane + ": " + index);
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
     * @param read read index
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
      final CompressionType compression = CompressionType
          .getCompressionTypeByFilename(undeterminedFiles.get(0).getName());

      // Create the writers
      // final Map<Pattern, FastqWriter> writers =
      // createWriters(read, compression);

      final Map<String, Entity> writers = createWriters2(read, compression);

      System.out
          .println("reDemux method, nupmber index retrieve from command line "
              + this.newIndexes.size() + " with " + writers.size()
              + " create on fastq.");

      // Create an array with the pattern. An array is faster than a collection.
      final List<Pattern> patterns = new ArrayList<>();
      for (Map.Entry<String, Entity> e : writers.entrySet()) {
        patterns.addAll(e.getValue().getPatterns());
      }

      for (File file : undeterminedFiles) {

        final FastqReader reader = new FastqReader(createInputStream(file));

        for (ReadSequence rs : reader) {

          // Do not use IlluminaReadId class because manual parsing is faster
          final String seqName = rs.getName();
          final String index = seqName.substring(seqName.lastIndexOf(':') + 1);

          for (Pattern p : patterns) {
            if (p.matcher(index).matches()) {

              for (Map.Entry<String, Entity> e : writers.entrySet()) {

                if (e.getValue().getPatterns().contains(p)) {
                  e.getValue().getFw().write(rs);
                  // writers.get(p).write(rs);
                  // status.add(p);
                  break;
                }
              }
            }
          }
        }
        reader.throwException();
        reader.close();

        // Close writers
        for (Entity e : writers.values()) {
          e.getFw().close();
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
        final CompressionType compression)
        throws FileNotFoundException, IOException {

      final Map<Pattern, FastqWriter> result = new HashMap<>();

      for (Map.Entry<Pattern, Sample> e : this.newIndexes.entrySet()) {

        final String sampleProject = e.getValue().getSampleProject();
        final String sampleName = e.getValue().getDemultiplexingName();
        final String sampleIndex = e.getValue().getIndex1();

        // Define the output directory
        final File subdir = new File(this.outputDir, "Project_"
            + sampleProject + File.separator + "Sample_" + sampleName);

        // Create output directory if not exists
        if (!subdir.isDirectory()) {

          if (!subdir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + subdir);
          }
        }

        // Define the output file
        final File file = new File(subdir,
            sampleName
                + "_" + sampleIndex + "_L00" + lane + "_R" + read + "_redemux_"
                + e.getKey().pattern() + ".fastq" + compression.getExtension());

        final OutputStream out =
            compression.createOutputStream(new FileOutputStream(file));

        result.put(e.getKey(), new FastqWriter(out));
      }

      return result;
    }

    private Map<String, Entity> createWriters2(final int read,
        final CompressionType compression)
        throws FileNotFoundException, IOException {

      final Map<String, Entity> result = new HashMap<>();

      for (Map.Entry<Pattern, Sample> e : this.newIndexes.entrySet()) {

        final String sampleProject = e.getValue().getSampleProject();
        final String sampleName = e.getValue().getDemultiplexingName();
        final String sampleIndex = e.getValue().getIndex1();

        // Define the output directory
        final File subdir = new File(this.outputDir, "Project_"
            + sampleProject + File.separator + "Sample_" + sampleName);

        // Create output directory if not exists
        if (!subdir.isDirectory()) {

          if (!subdir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + subdir);
          }
        }

        // Define the output file
        final File file = new File(subdir,
            sampleName
                + "_" + sampleIndex + "_L00" + lane + "_R" + read
                + "_redemux_.fastq" + compression.getExtension());

        final OutputStream out =
            compression.createOutputStream(new FileOutputStream(file));

        if (!result.containsKey(sampleName)) {
          result.put(sampleName,
              new Entity(new FastqWriter(out), e.getValue()));
        }

        result.get(sampleName).addPattern(e.getKey());
      }

      return result;
    }

    //
    // Internal Class
    //
    static final class Entity {

      private FastqWriter fw;
      private List<Pattern> patterns;
      private Sample cs;

      void addPattern(final Pattern p) {

        if (patterns.contains(p))
          throw new RuntimeException();

        this.patterns.add(p);
      }

      public FastqWriter getFw() {
        return fw;
      }

      public List<Pattern> getPatterns() {
        return patterns;
      }

      public Sample getCs() {
        return cs;
      }

      Entity(final FastqWriter fw, final Sample cs) {
        this.fw = fw;
        this.patterns = new ArrayList<>();
        this.cs = cs;
      }
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
     * @param samplesheet Bcl2fastq samplesheet object
     * @param lane lane to re-demultiplex
     * @param inputDir input directory
     * @param outputDir output directory
     */
    public ReDemuxLane(final SampleSheet samplesheet, final int lane,
        final File inputDir, final File outputDir) {

      this.samplesheet = samplesheet;
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

    // TODO extract max lane number from samplesheet
    Preconditions.checkArgument(lane >= 1 && lane <= 8,
        "Invalid lane number: " + lane);

    final ReDemuxLane reDemuxLane;

    if (!this.lanesToRedemux.containsKey(lane)) {
      reDemuxLane =
          new ReDemuxLane(sampleSheet, lane, inputDir, this.outputDir);
      this.lanesToRedemux.put(lane, reDemuxLane);
    } else
      reDemuxLane = this.lanesToRedemux.get(lane);

    reDemuxLane.addIndex(index);
  }

  /**
   * Launch the re-demultiplexing.
   * @throws IOException if an IO error occurs while re-demultiplexing
   * @throws BadBioEntryException if an FASTQ entry read is invald
   * @throws IOException
   * @throws BadBioEntryException
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
  private static int mismatches(final String a, final String b) {

    requireNonNull(a, "a cannot be null");
    requireNonNull(b, "b cannot be null");
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
   * @param samplesheet Bcl2fastq samplesheet object
   * @param outputDir output directory
   */
  public ReDemux(final File baseDir, final SampleSheet samplesheet,
      final File outputDir) {

    requireNonNull(samplesheet,
        "samplesheet argument cannot be null");
    requireNonNull(baseDir, "baseDir argument cannot be null");
    this.sampleSheet = samplesheet;
    this.inputDir = baseDir;
    this.outputDir = outputDir;
  }

  //
  // Static methods
  //

  public static void redemultiplex(final File samplesheetFile,
      final String bcl2fastqVersion, final List<String> lanesAndIndex,
      final File outputDir) throws FileNotFoundException, IOException,
      AozanException, BadBioEntryException {

    requireNonNull(samplesheetFile,
        "samplesheetFile cannot be null");
    requireNonNull(lanesAndIndex, "laneAndIndex cannot be null");
    requireNonNull(outputDir, "output directory cannot be null");

    // Load samplesheet
    final SampleSheet samplesheet =
        new SampleSheetCSVReader(samplesheetFile).read();

    // Create ReDemux object
    ReDemux rd =
        new ReDemux(samplesheetFile.getParentFile(), samplesheet, outputDir);

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
      IOException, AozanException, BadBioEntryException {

    final List<String> list = Arrays.asList(args);

    if (list.size() < 2) {
      System.err.println("Syntax: redemux rundir laneindex...");
      System.exit(1);
    }

    redemultiplex(new File(list.get(0)), list.get(1),
        list.subList(1, list.size()), new File(System.getProperty("user.dir")));
  }

}
