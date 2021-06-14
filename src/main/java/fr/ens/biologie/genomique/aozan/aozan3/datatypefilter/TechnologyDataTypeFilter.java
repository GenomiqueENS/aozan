package fr.ens.biologie.genomique.aozan.aozan3.datatypefilter;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology;

/**
 * This class define a DataTypeFilter that require or not partial data..
 * @author Laurent Jourdren
 * @since 3.0
 */
public class TechnologyDataTypeFilter implements DataTypeFilter {

  private final SequencingTechnology technology;

  @Override
  public boolean accept(DataType type) {

    if (type == null) {
      return false;
    }

    return this.technology == type.getSequencingTechnology();
  }

  @Override
  public String toString() {
    return "TechnologyDataTypeFilter [technology=" + technology + "]";
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   * @param requiredCategory required DataType
   */
  public TechnologyDataTypeFilter(SequencingTechnology technology) {

    this.technology = technology;
  }
}
