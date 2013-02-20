package fr.ens.transcriptome.aozan.io;

import java.io.File;
import java.io.FileFilter;

public class FastqSample {

  private FastqStorage fastqStorage;

  private int read1 = 0;
  private int read2 = 0;
  private int lane = 0;
  private String sampleName;
  private String projectName;
  private String runFastqPath;
  private String keyFastqFiles;
  private File[] fastqFiles;

  // TODO to remove
  private String index;

  public boolean equals(FastqSample ref) {
    return this.sampleName.equals(ref.sampleName);
  }

  public String getPrefixRundata() {
    return "."
        + this.lane + ".sample." + this.sampleName + ".read" + this.read1 + "."
        + this.sampleName;
  }

  public String getFilePathInRun() {
    StringBuilder s = new StringBuilder();

    s.append("/Project_");
    s.append(this.projectName);
    s.append("/Sample_");
    s.append(this.sampleName);
    // prefix file
    s.append(String.format("/%s_%s_L%03d_R%d_", sampleName, "".equals(index)
        ? "NoIndex" : index, lane, read1));
    // format file
    s.append("001.fastq.bz2");

    return s.toString();
  }

  public boolean isUncompressedNeeded() {
    return true;
  }

  public long getUncompressedSize() {
    // according to type of compressionExtension
    long sizeFastqFiles = 0;

    for (File f : fastqFiles) {
      sizeFastqFiles += f.length();
    }

    return (long) (sizeFastqFiles * fastqStorage.getCoefficientUncompress());
  }

  /**
   * Set the directory to the file
   * @return
   */
  public File casavaOutputDir() {

    return new File(this.runFastqPath
        + "/Project_" + projectName + "/Sample_" + sampleName);
  }

  /**
   * Set the prefix of the file of read1
   * @return
   */
  public String prefixFileName(int read) {
    return String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
        ? "NoIndex" : index, lane, read);
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix
   * @return an array of abstract pathnames
   */
  private File[] createListFastqFiles(final int read) {

    return new File(casavaOutputDir() + "/").listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return pathname.length() > 0
            && pathname.getName().startsWith(prefixFileName(read))
            && pathname.getName().endsWith(
                fastqStorage.getCompressionExtension());
      }
    });
  }

  //
  // Getter
  //

  public int getRead() {
    return this.read1;
  }

  public int getLane() {
    return this.lane;
  }

  public String getProjectName() {
    return this.projectName;
  }

  public String getSampleName() {
    return this.sampleName;
  }

  public File[] getFastqFiles() {
    return this.fastqFiles;
  }

  public File[] getFastqFilesRead2() {
    if (read2 == 0)
      return null;

    return createListFastqFiles(read2);
  }

  public String getKeyFastqFiles() {
    return this.keyFastqFiles;
  }

  //
  // Constructor
  //

  public FastqSample(final String casavaOutputPath, final int read,
      final int lane, final String sampleName, final String projectName,
      final String index) {

    this.fastqStorage = FastqStorage.getInstance();

    this.read1 = read;
    this.lane = lane;
    this.sampleName = sampleName;
    this.projectName = projectName;
    this.index = index;

    this.runFastqPath = casavaOutputPath;
    this.fastqFiles = createListFastqFiles(read1);

    System.out.println("create fastqSample for "
        + sampleName + "nb fastqFiles " + fastqFiles.length);

    if (fastqFiles == null || fastqFiles.length == 0) {
      this.keyFastqFiles = null;

    } else {
      this.keyFastqFiles = fastqStorage.keyFiles(this.fastqFiles);

    }
  }

}
