package fr.ens.biologie.genomique.aozan.aozan3.datatypefilter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;

/**
 * This class define a filter that combine several other filters.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class MultiDataTypeFilter implements DataTypeFilter {

  private final List<DataTypeFilter> filters;

  @Override
  public boolean accept(DataType type) {

    for (DataTypeFilter filter : this.filters) {

      if (filter == null) {
        continue;
      }

      if (!filter.accept(type)) {
        return false;
      }

    }

    return true;
  }

  @Override
  public String toString() {
    return "MultiDataTypeFilter [filters=" + filters + "]";
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param filters filters to use
   */
  public MultiDataTypeFilter(DataTypeFilter... filters) {

    Objects.requireNonNull(filters);
    this.filters = Arrays.asList(filters);
  }

}
