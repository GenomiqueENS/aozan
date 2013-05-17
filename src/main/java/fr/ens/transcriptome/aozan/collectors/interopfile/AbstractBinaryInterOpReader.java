package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import fr.ens.transcriptome.aozan.RunData;

abstract class AbstractBinaryInterOpReader implements BinaryInterOpReader {

  protected int lanes;
  protected int reads;
  protected int tiles;

  protected int read1CyclesCumul;
  protected int read2CyclesCumul;
  protected int read3CyclesCumul;

  public void collect(final RunData data) {

    System.out.println("COLLECT");

    this.lanes = data.getInt("run.info.flow.cell.lane.count");
    this.reads = data.getInt("run.info.read.count");
    this.tiles =
        data.getInt("run.info.flow.cell.tile.count")
            * data.getInt("run.info.flow.cell.surface.count")
            * data.getInt("run.info.flow.cell.swath.count");
    this.read1CyclesCumul = data.getInt("run.info.read1.cycles");

    this.read2CyclesCumul =
        (this.reads >= 2) ? data.getInt("run.info.read2.cycles")
            + this.read1CyclesCumul : -1;
    this.read3CyclesCumul =
        (this.reads >= 3) ? data.getInt("run.info.read3.cycles")
            + this.read2CyclesCumul : -1;

    System.out.println(lanes
        + " value seuil reads " + read1CyclesCumul + " -- " + read2CyclesCumul
        + " -- " + read3CyclesCumul);

    for (int read = 1; read <= reads; read++) {
      // TODO to define
      data.put("read" + read + ".density.ratio", "0.3472222");
      String s =
          data.getBoolean("run.info.read" + read + ".indexed") ? "(Index)" : "";
      data.put("read" + read + ".type", s);
    }
  }

  public static <T> Collection<T> makeCollection(final Iterator<T> i) {
    final Collection<T> c = new LinkedList<T>();
    while (i.hasNext()) {
      c.add(i.next());
    }
    return c;
  }

}
