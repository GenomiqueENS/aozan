package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Iterator;

import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;

interface BinaryIteratorReader extends Iterator<IlluminaMetrics> {

  public boolean hasNext();

  public IlluminaMetrics next();

  public void remove();

}
