package fr.ens.biologie.genomique.aozan.collectors.interop;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;

public class TileMetricsVersion3Reader
    extends AbstractBinaryFileReader<TileMetricsVersion3> {

  public static final String NAME = "TileMetricsOut";

  public static final String TILE_METRICS_FILE = "TileMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 15;
  private static final int EXPECTED_VERSION = 3;

  private float tileArea;

  @Override
  protected File getMetricsFile() {
    return new File(getDirPathInterOP(), TILE_METRICS_FILE);
  }

  @Override
  protected int getExpectedRecordSize(int version) {
    return EXPECTED_RECORD_SIZE;
  }

  @Override
  protected Set<Integer> getExpectedVersions() {
    return Collections.singleton(EXPECTED_VERSION);

  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  protected void readOptionalFlag(ByteBuffer bb, int version) {

    this.tileArea = bb.getFloat();
  }

  @Override
  protected void readMetricRecord(final List<TileMetricsVersion3> collection,
      final ByteBuffer bb, final int version) {

    collection.add(new TileMetricsVersion3(bb, this.tileArea));
  }

  //
  // Constructor
  //

  TileMetricsVersion3Reader(final File dirPath) throws AozanException {
    super(dirPath);
  }

}
