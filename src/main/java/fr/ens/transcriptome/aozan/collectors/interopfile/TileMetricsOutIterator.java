package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import net.sf.picard.util.UnsignedTypeUtil;

public class TileMetricsOutIterator extends AbstractBinaryIteratorReader {

  public final String NAME = "TileMetricsOut.bin";

  public static final String tileMetricsOutFile = dirInterOp
      + "TileMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 10;
  private static final int EXPECTED_VERSION = 2;

  public TileMetricsOutIterator() {
    super(new File(tileMetricsOutFile), EXPECTED_RECORD_SIZE, EXPECTED_VERSION);
  }

  public String getName() {
    return this.NAME;
  }

  public IlluminaMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaTileMetrics(bbIterator.next());
  }

  //
  // Internal class
  //

  class IlluminaTileMetrics extends IlluminaMetrics {

    private final int metricCode;
    private final float metricValue;

    public IlluminaTileMetrics(final ByteBuffer bb) {
      super(bb);
      metricCode = UnsignedTypeUtil.uShortToInt(bb.getShort());
      metricValue = bb.getFloat();

    }

    public int getMetricCode() {
      return metricCode;
    }

    public float getMetricValue() {
      return metricValue;
    }

    @Override
    public String toString() {
      return String.format("lane %s tile %s code %s value %.04f", laneNumber,
          tileNumber, metricCode, metricValue);
    }
  }
}
