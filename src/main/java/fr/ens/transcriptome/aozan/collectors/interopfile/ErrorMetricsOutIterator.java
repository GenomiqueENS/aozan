package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import net.sf.picard.util.UnsignedTypeUtil;

public class ErrorMetricsOutIterator extends AbstractBinaryIteratorReader {

  public final String NAME = "ErrorMetricsOut.bin";

  public static String errorMetricsOutFile = dirInterOp + "ErrorMetricsOut.bin";
  private static final int EXPECTED_RECORD_SIZE = 30;
  private static final int EXPECTED_VERSION = 3;

  public ErrorMetricsOutIterator() {
    super(new File(errorMetricsOutFile), EXPECTED_RECORD_SIZE, EXPECTED_VERSION);
  }

  public String getName() {
    return this.NAME;
  }

  public static boolean errorMetricsOutFileExists() {
    return new File(errorMetricsOutFile).exists();
  }

  @Override
  public IlluminaErrorMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaErrorMetrics(bbIterator.next());
  }

  //
  // Internal class
  //

  public static class IlluminaErrorMetrics extends IlluminaMetrics {

    static int maxTile = 0;
    static int minTile = Integer.MAX_VALUE;

    static int maxCycle = 0;
    static int minCycle = Integer.MAX_VALUE;

    private final int cycleNumber; // uint16
    private final float errorRate; // float
    private final int numberPerfectReads; // uint32
    private final int numberReadsOneError; // uint32
    private final int numberReadsTwoErrors; // uint32
    private final int numberReadsThreeErrors; // uint32
    private final int numberReadsFourErrors; // uint32
    private final int sumReadsErrors;

    public IlluminaErrorMetrics(final ByteBuffer bb) {
      super(bb);

      this.cycleNumber = UnsignedTypeUtil.uShortToInt(bb.getShort());

      this.errorRate = bb.getFloat();
      this.numberPerfectReads = bb.getInt();
      this.numberReadsOneError = bb.getInt();
      this.numberReadsTwoErrors = bb.getInt();
      this.numberReadsThreeErrors = bb.getInt();
      this.numberReadsFourErrors = bb.getInt();

      this.sumReadsErrors =
          this.numberReadsOneError
              + this.numberReadsTwoErrors + this.numberReadsThreeErrors
              + this.numberReadsFourErrors;

      maxTile = (tileNumber > maxTile) ? tileNumber : maxTile;
      minTile = (tileNumber < minTile) ? tileNumber : minTile;

      maxCycle = (cycleNumber > maxCycle) ? cycleNumber : maxCycle;
      minCycle = (cycleNumber < minCycle) ? cycleNumber : minCycle;
    }

    public int getCycleNumber() {
      return cycleNumber;
    }

    public float getErrorRate() {
      return errorRate;
    }

    public int getNumberPerfectReads() {
      return this.numberPerfectReads;
    }

    public int getNumberReadsOneError() {
      return this.numberReadsOneError;
    }

    public int getNumberReadsTwoError() {
      return this.numberReadsTwoErrors;
    }

    public int getNumberReadsThreeError() {
      return this.numberReadsThreeErrors;
    }

    public int getNumberReadsFourError() {
      return this.numberReadsFourErrors;
    }

    public int noError() {
      return this.sumReadsErrors ;
    }

    public int getMaxTile() {
      return maxTile;
    }

    public int getMinTile() {
      return minTile;
    }

    public int getMaxCycle() {
      return maxCycle;
    }

    public int getMinCycle() {
      return minCycle;
    }

    @Override
    public String toString() {
      return String.format("%s\t%s\t%s\t%.2f\t%s\t%s\t%s\t%s\t%s", laneNumber,
          tileNumber, cycleNumber, errorRate, numberPerfectReads,
          numberReadsOneError, numberReadsTwoErrors, numberReadsThreeErrors,
          numberReadsFourErrors);
    }
  }
}
