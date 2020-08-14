package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;

/**
 * This class define a registry for data storages.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DataStorageRegistry {

  private Map<String, DataStorage> registry = new HashMap<>();

  /**
   * Add a storage to the registry
   * @param name
   * @param storage
   */
  public void add(String name, DataStorage storage) {

    checkName(name);
    requireNonNull(storage);

    this.registry.put(name, storage);
  }

  /**
   * Get a data storage from its name
   * @param name name of the data storage
   * @return a DataStorage object
   * @throws Aozan3Exception if the data storage does not exists
   */
  public DataStorage get(String name) throws Aozan3Exception {

    if (!exists(name)) {
      throw new Aozan3Exception("Unknown data storage: " + name);
    }

    return this.registry.get(name);
  }

  /**
   * Remove a data storage.
   * @param name name of the data storage to remove
   * @return true if the storage has been removed
   */
  public boolean remove(String name) {

    checkName(name);

    return this.registry.remove(name) != null;
  }

  /**
   * Test if a storage exists.
   * @param name name of the storage
   * @return true if the storage exists
   */
  public boolean exists(String name) {

    checkName(name);

    return this.registry.containsKey(name);
  }

  /**
   * Get the names of registered storages.
   * @return a set with the names of the registered storages
   */
  public Set<String> names() {

    return this.registry.keySet();
  }

  //
  // Private methods
  //

  private static void checkName(String name) {

    requireNonNull(name);

    if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("Storage name cannot be empty");
    }

  }

}
