package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.DataType;
import fr.ens.biologie.genomique.aozan.aozan3.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.Category;

/**
 * This class define InputData for processors.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class InputData {

  Map<Category, RunData> map = new HashMap<>();

  /**
   * Add a run data.
   * @param runData run data to add
   */
  public void add(RunData runData) {

    requireNonNull(runData);
    map.put(runData.getType().getCategory(), runData);
  }

  /**
   * Add a run data.
   * @param runData run data to add
   */
  public void add(Collection<RunData> collection) {

    requireNonNull(collection);

    for (RunData r : collection) {
      add(r);
    }
  }

  /**
   * Add all the content of an existing collection to the current collection
   * @param collection collection to add
   */
  public void add(InputData collection) {

    requireNonNull(collection);

    for (RunData runData : collection.entries()) {
      add(runData);
    }
  }

  /**
   * Get all the entries in the collection
   * @return a HashSet with a copy of all entries
   */
  public Set<RunData> entries() {

    return new HashSet<RunData>(this.map.values());
  }

  /**
   * Filter the collection.
   * @param filter the filter to use
   * @return a new collection
   */
  public InputData filter(DataTypeFilter filter) {

    requireNonNull(filter);

    InputData result = new InputData();

    for (RunData runData : this.map.values()) {
      if (filter.accept(runData.getType())) {
        result.add(runData);
      }
    }

    return result;
  }

  /**
   * Clear the collection.
   */
  public void clear() {
    this.map.clear();
  }

  /**
   * Get the size of the collection.
   * @return the size of the collection
   */
  public int size() {
    return this.map.size();
  }

  /**
   * Test if the collection is empty.
   * @return true if the collection is empty
   */
  public boolean isEmpty() {
    return this.map.isEmpty();
  }

  /**
   * Test if the size of the collection is 1.
   * @return true if there is only one element in the collection
   */
  public boolean isOneElement() {
    return this.map.size() == 1;
  }

  /**
   * Get the only RunData element in the collection
   * @return the only RunData element in the collection
   * @throws IllegalStateException if there is not one element in the collection
   */
  public RunData getTheOnlyElement() {

    if (this.map.size() != 1) {
      throw new IllegalStateException(
          "There is not one element in the collection");
    }

    return this.map.values().iterator().next();
  }

  /**
   * Get the first RunData with a matching data type.
   * @param dataType the data type of the run data
   * @return a RunData object
   * @throws NoSuchElementException if the element does not exists
   */
  public RunData get(DataType dataType) {

    requireNonNull(dataType);

    for (RunData r : this.map.values()) {
      if (dataType.equals(r.getType())) {
        return r;
      }
    }

    throw new NoSuchElementException();
  }

  /**
   * Get a RunData from its data type.
   * @param dataType the data type of the run data
   * @return a RunData object
   * @throws NoSuchElementException if the element does not exists
   */
  public RunData get(DataType.Category category) {

    requireNonNull(category);

    if (!this.map.containsKey(category)) {
      throw new NoSuchElementException();
    }

    return this.map.get(category);

  }

  public RunData getLastRunData() {

    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    List<Category> categoryList =
        new ArrayList<>(Arrays.asList(Category.values()));
    Collections.reverse(categoryList);

    for (Category c : categoryList) {

      if (this.map.containsKey(c)) {
        return this.map.get(c);
      }

    }

    throw new NoSuchElementException();
  }

  //
  // Constructors
  //

  /**
   * This constructor create an empty InputData.
   */
  public InputData() {
  }

  /**
   * Create a new object with one element
   * @param runData element to add
   */
  public InputData(RunData runData) {
    add(runData);
  }

  /**
   * Create a new object with the content of a collection of RunData objects.
   * @param runData the element to add
   */
  public InputData(Collection<RunData> runData) {
    add(runData);
  }

}
