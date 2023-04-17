package fr.ens.biologie.genomique.aozan.aozan3.datatypefilter;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;

/**
 * This class define a DataTypeFilter that require partial or not DataType.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class PartialDataTypeFilter implements DataTypeFilter {

  private final boolean partial;

  @Override
  public boolean accept(DataType type) {

    if (type == null) {
      return false;
    }

    return this.partial == type.isPartialData();
  }

  @Override
  public String toString() {
    return "PartialDataTypeFilter [partial=" + partial + "]";
  }

  //
  // Constructors
  //

  /**
   * Consctructor.
   * @param partial partial data
   */
  public PartialDataTypeFilter(boolean partial) {

    this.partial = partial;
  }

}
