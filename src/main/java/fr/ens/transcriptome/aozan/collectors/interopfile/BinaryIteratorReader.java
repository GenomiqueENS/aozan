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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Iterator;

import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;

/**
 * This interface define an iterator on Illumina Metrics for reading binary files
 * from the InterOp directory. It allow to parse all records.
 * @author Sandrine Perrin
 * @since 1.1
 */

interface BinaryIteratorReader extends Iterator<IlluminaMetrics> {

  @Override
  public boolean hasNext();

  @Override
  public IlluminaMetrics next();

  @Override
  public void remove();

}
