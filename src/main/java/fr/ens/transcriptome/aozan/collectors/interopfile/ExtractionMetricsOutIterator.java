package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import net.sf.picard.util.UnsignedTypeUtil;

public class ExtractionMetricsOutIterator extends AbstractBinaryIteratorReader {

  public final String NAME = "ExtractionMetricsOut.bin";

  public static final String extractMetricsOutFile = dirInterOp
      + "ExtractionMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 38;
  private static final int EXPECTED_VERSION = 2;

  public ExtractionMetricsOutIterator() {
    super(new File(extractMetricsOutFile), EXPECTED_RECORD_SIZE,
        EXPECTED_VERSION);
  }

  public String getName() {
    return this.NAME;
  }

  public IlluminaMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaIntensitiesMetrics(bbIterator.next());
  }

  //
  // Internal Class
  //

  public static class IlluminaIntensitiesMetrics extends IlluminaMetrics {

    static int maxTile = 0;
    static int minTile = Integer.MAX_VALUE;

    static int maxCycle = 0;
    static int minCycle = Integer.MAX_VALUE;

    static final int BASE_A = 0;
    static final int BASE_C = 0;
    static final int BASE_G = 0;
    static final int BASE_T = 0;

    private final int cycleNumber; // uint16
    private final float[] fwhm = new float[4]; // A C G T - float
    private final int[] intensities = new int[4]; // A C G T - uint16
    private int sumIntensities = 0;

    public IlluminaIntensitiesMetrics(final ByteBuffer bb) {
      super(bb);

      this.cycleNumber = UnsignedTypeUtil.uShortToInt(bb.getShort());

      for (int i = 0; i < 4; i++) {
        this.fwhm[i] = bb.getFloat();
      }

      for (int i = 0; i < 4; i++) {
        this.intensities[i] = UnsignedTypeUtil.uShortToInt(bb.getShort());
        this.sumIntensities += this.intensities[i];
      }
      ByteBuffer dateCif = bb.get(new byte[8]);

      maxTile = (tileNumber > maxTile) ? tileNumber : maxTile;
      minTile = (tileNumber < minTile) ? tileNumber : minTile;

      maxCycle = (cycleNumber > maxCycle) ? cycleNumber : maxCycle;
      minCycle = (cycleNumber < minCycle) ? cycleNumber : minCycle;

    }

    public int getCycleNumber() {
      return cycleNumber;
    }

    public float[] getFwhm() {
      return fwhm;
    }

    public int[] getIntensities() {
      return intensities;
    }

    public int getSumIntensitiesByChannel() {
      int sum = 0;
      for (int n = 0; n < intensities.length; n++) {
        sum += intensities[n];
//        System.out.print(n + " - " + intensities[n]);
      }
//      System.out.println();
      return sum/4;
    }
  }
}
