package fr.ens.biologie.genomique.aozan.collectors.interop;

import static fr.ens.biologie.genomique.aozan.collectors.interop.AbstractBinaryFileReader.uShortToInt;
import static fr.ens.biologie.genomique.aozan.collectors.interop.AbstractBinaryFileReader.uIntToLong;
import static fr.ens.biologie.genomique.aozan.collectors.interop.AbstractBinaryFileReader.uByteToInt;

import java.nio.ByteBuffer;

public class TileMetricsVersion3 {

  enum Type {
    TILE, READ, ZERO
  };

  private final Type type;

  private final float clusterCount;
  private final float pfClusterCount;
  private final long readNumber;
  private final float prcAligned;

  private final int laneNumber; // uint16
  private final long tileNumber; // uint32

  //
  // Constructor
  //

  public Type getType() {
    return type;
  }

  public float getClusterCount() {
    return clusterCount;
  }

  public float getPfClusterCount() {
    return pfClusterCount;
  }

  public long getReadNumber() {
    return readNumber;
  }

  public float getPrcAligned() {
    return prcAligned;
  }

  public int getLaneNumber() {
    return laneNumber;
  }

  public long getTileNumber() {
    return tileNumber;
  }

  /**
   * Constructor. One record countReads on the ByteBuffer.
   * @param bb ByteBuffer who read one record
   */
  TileMetricsVersion3(final ByteBuffer bb, float tileArea) {

    this.laneNumber = uShortToInt(bb);
    this.tileNumber = uIntToLong(bb);

    int metricCode = uByteToInt(bb);

    switch (metricCode) {

    case 't':
      this.type = Type.TILE;
      this.clusterCount = bb.getFloat();
      this.pfClusterCount = bb.getFloat();
      this.readNumber = 0;
      this.prcAligned = 0;
      break;

    case 'r':
      this.type = Type.READ;
      this.clusterCount = 0;
      this.pfClusterCount = 0;
      this.readNumber = uIntToLong(bb);
      this.prcAligned = bb.getFloat();
      break;

    case 0:
      this.type = Type.ZERO;
      this.clusterCount = 0;
      this.pfClusterCount = 0;
      this.readNumber = 0;
      this.prcAligned = 0;
      break;

    default:
      throw new IllegalStateException();
    }
  }

}
