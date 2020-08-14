package fr.ens.biologie.genomique.aozan.aozan3.datatypefilter;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;

/**
 * This class define a DataTypeFilter that require a specific DataType.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SimpleDataTypeFilter implements DataTypeFilter {

  private final DataType dataType;

  @Override
  public boolean accept(DataType type) {

    return dataType == type;
  }

  //
  // Constructors
  //

  /**
   * Consctructor.
   * @param requiredDataType required DataType
   */
  public SimpleDataTypeFilter(DataType requiredDataType) {

    requireNonNull(requiredDataType);
    this.dataType = requiredDataType;
  }

}
