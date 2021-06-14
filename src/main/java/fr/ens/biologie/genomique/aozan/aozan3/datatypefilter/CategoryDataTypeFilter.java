package fr.ens.biologie.genomique.aozan.aozan3.datatypefilter;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.Category;

/**
 * This class define a DataTypeFilter that require a specific DataType.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class CategoryDataTypeFilter implements DataTypeFilter {

  private final Category category;

  @Override
  public boolean accept(DataType type) {

    if (type == null) {
      return false;
    }

    return this.category == type.getCategory();
  }

  @Override
  public String toString() {
    return "CategoryDataTypeFilter [category=" + category + "]";
  }

  //
  // Constructors
  //

  /**
   * Consctructor.
   * @param requiredCategory required DataType
   */
  public CategoryDataTypeFilter(Category requiredCategory) {

    requireNonNull(requiredCategory);
    this.category = requiredCategory;
  }

}
