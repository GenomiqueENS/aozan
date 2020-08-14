package fr.ens.biologie.genomique.aozan.aozan3.datatypefilter;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;

/**
 * This interface define a filter for DataType.
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface DataTypeFilter {

  /**
   * Test if a data type is valid.
   * @param type data type
   * @return true if the data type is accepted
   */
  boolean accept(DataType type);

}
