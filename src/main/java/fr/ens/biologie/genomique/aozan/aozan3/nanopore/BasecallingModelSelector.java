package fr.ens.biologie.genomique.aozan.aozan3.nanopore;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.kenetre.util.Version;

/**
 * This class allow to select the model to use with Dorado from parameters like
 * flowcell or kit references.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class BasecallingModelSelector {

  private final Set<Model> models = new HashSet<>();
  private final Path modelsPath;

  private String analyteTypeFilter;
  private String poreTypeFilter;
  private String chemistryTypeFilter;
  private String translocationSpeedFilter;
  private String samplingFrequencyFilter;
  private String modelTypeFilter;
  private String versionFilter;
  private Set<String> compatibleModificationsFilter;

  static class Model {

    private final String name;
    private final String analyteType;
    private final String poreType;
    private final String chemistryType;
    private final String translocationSpeed;
    private final String samplingFrequency;
    private final String modelType;
    private final String version;
    private final String modificationModelVersion;
    private final Set<String> compatibleModifications = new HashSet<>();
    private final boolean stereo;

    @Override
    public String toString() {
      return "Model [name="
          + name + ", analyteType=" + analyteType + ", poreType=" + poreType
          + ", chemistryType=" + chemistryType + ", translocationSpeed="
          + translocationSpeed + ", samplingFrequency=" + samplingFrequency
          + ", modelType=" + modelType + ", version=" + version
          + ", modificationModelVersion=" + modificationModelVersion
          + ", compatibleModifications=" + compatibleModifications + ", stereo="
          + stereo + "]";
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.analyteType, this.poreType,
          this.chemistryType, this.translocationSpeed, this.modelType,
          this.version, this.modificationModelVersion,
          this.compatibleModifications);
    }

    @Override
    public boolean equals(Object obj) {

      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Model other = (Model) obj;

      return Objects.equals(this.name, other.name);
    }

    Model(String name) {

      requireNonNull(name);

      this.name = name.trim();
      List<String> mainParts =
          Splitter.on('@').trimResults().splitToList(name.toLowerCase());

      if (mainParts.size() < 2 && mainParts.size() > 3) {
        throw new IllegalArgumentException("Invalid model name1: " + name);
      }

      List<String> elements =
          Splitter.on('_').trimResults().splitToList(mainParts.get(0));

      this.analyteType = elements.get(0);
      if (!this.analyteType.equals("dna")
          && !this.analyteType.startsWith("rna")) {
        throw new IllegalArgumentException("Invalid analyte type: " + name);
      }

      String samplingFrequency = null;

      if (this.analyteType.equals("dna")) {

        // DNA case
        if (elements.size() < 4 && elements.size() > 5) {
          throw new IllegalArgumentException("Invalid model name: " + name);
        }

        String poreType = null;
        String chemistryType = null;
        String translocationSpeed = null;
        String modelType = null;
        boolean stereo = false;

        for (String e : elements.subList(1, elements.size())) {

          switch (e) {

          case "r10.4.1":
          case "r9.4.1":
            poreType = e;
            break;

          case "e8.2":
          case "e8":
            chemistryType = e;
            break;

          case "260bps":
          case "400bps":
            translocationSpeed = e;
            break;

          case "4khz":
          case "5khz":
            samplingFrequency = e;
            break;

          case "fast":
          case "hac":
          case "sup":
            modelType = e;
            break;

          case "stereo":
            stereo = true;
            break;

          default:
            throw new IllegalArgumentException("Invalid model name: " + name);
          }
        }

        this.poreType = poreType;
        this.chemistryType = chemistryType;
        this.translocationSpeed = translocationSpeed;
        this.modelType = modelType;
        this.stereo = stereo;

      } else {

        // RNA case
        if (elements.size() != 3) {
          throw new IllegalArgumentException("Invalid model name3: " + name);
        }

        this.translocationSpeed = elements.get(1);
        switch (this.translocationSpeed) {
        case "130bps":
        case "70bps":
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid translocation speed: " + this.translocationSpeed);
        }

        this.modelType = elements.get(2);
        switch (this.modelType) {
        case "fast":
        case "hac":
        case "sup":
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid model typeX: " + this.modelType);
        }

        this.poreType = null;
        this.chemistryType = null;
        this.stereo = false;
      }

      elements = Splitter.on('_').trimResults().splitToList(mainParts.get(1));
      this.version = elements.get(0);
      for (int i = 1; i < elements.size(); i++) {
        this.compatibleModifications.add(elements.get(i));
      }

      if (mainParts.size() == 3) {
        this.modificationModelVersion = mainParts.get(2);
      } else {
        this.modificationModelVersion = null;
      }

      if (this.modificationModelVersion != null
          && this.compatibleModifications.isEmpty()) {
        throw new IllegalArgumentException("Invalid model name4: " + name);
      }

      if (samplingFrequency == null) {
        switch (this.version) {
        case "v3.5.2":
        case "v4.0.0":
        case "v4.1.0":
          this.samplingFrequency = "4khz";
          break;
        default:
          this.samplingFrequency = "5khz";
        }
      } else {
        this.samplingFrequency = samplingFrequency;
      }

    }

  }

  private void listModels(Path modelsPath) {

    for (File f : modelsPath.toFile().listFiles()) {

      if (f.isDirectory()) {
        this.models.add(new Model(f.getName()));
      }
    }
  }

  /**
   * Get the pore type from the flowcell reference
   * @param flowcellRef the flow cell reference
   * @return the pore type
   */
  public static String getPoreType(String flowcellRef) {

    requireNonNull(flowcellRef);

    switch (flowcellRef.toUpperCase().trim()) {

    case "FLO-FLG001":
    case "FLO-MIN106":
    case "FLO-MINSP6":
    case "FLO-PRO001":
    case "FLO-PRO002":
    case "FLO-PRO002-ECO":
    case "FLO-PRO002M":
      return "r9.4.1";

    case "FLO-FLG114":
    case "FLO-MIN114":
    case "FLO-PRO114":
    case "FLO-PRO114M":
      return "r10.4.1";

    case "FLO-MIN107":
    case "FLO-MIN110":
    case "FLO-FLG111":
    case "FLO-MIN111":
    case "FLO-PRO111":
    case "FLO-MIN112":
    case "FLO-PRO112":
    case "FLO-PRO112M":
      throw new IllegalArgumentException(
          "Flowcell ref not handled by Dorado: " + flowcellRef);

    default:
      throw new IllegalArgumentException(
          "Unknown flowcell reference: " + flowcellRef);
    }

  }

  /**
   * Get the chemistry type from the kit reference
   * @param kit the kit reference
   * @return the chemistry type
   */
  public static String getChemistryType(String kit) {

    requireNonNull(kit);

    switch (kit.toUpperCase().trim()) {

    case "SQK-LSK111":
    case "SQK-LSK111-XL":
    case "SQK-MLK111-96-XL":
    case "SQK-NBD111-24":
    case "SQK-NBD111-96":
    case "SQK-PCB111-24":
    case "SQK-PCS111":
    case "SQK-RBK111-24":
    case "SQK-RBK111-96":
    case "SQK-LSK112":
    case "SQK-LSK112-XL":
    case "SQK-NBD112-24":
    case "SQK-NBD112-96":
    case "SQK-RAD112":
    case "SQK-RBK112-24":
    case "SQK-RBK112-96":
      return "e8";

    case "SQK-LSK114":
    case "SQK-LSK114-XL":
    case "SQK-NBD114-24":
    case "SQK-NBD114-96":
    case "SQK-RAD114":
    case "SQK-RBK114-24":
    case "SQK-RBK114-96":
    case "SQK-RPB114-24":
    case "SQK-ULK114":
      return "e8.2";

    default:
      throw new IllegalArgumentException("Unknown kit reference: " + kit);
    }

  }

  //
  // Configuration of the filtering
  //

  /**
   * Add a filter based on the analyte type.
   * @param analyteType the analyte type
   * @return the object
   */
  public BasecallingModelSelector withAnalyteType(String analyteType) {

    if (analyteType == null || analyteType.isBlank()) {
      return this;
    }

    this.analyteTypeFilter = analyteType.trim().toLowerCase();
    return this;
  }

  /**
   * Add a filter based on the pore type.
   * @param poreType the pore type
   * @return the object
   */
  public BasecallingModelSelector withPoreType(String poreType) {

    if (poreType == null || poreType.isBlank()) {
      return this;
    }

    this.poreTypeFilter = poreType.trim().toLowerCase();
    return this;
  }

  /**
   * Add a filter based on the chemistry type.
   * @param chemistryType the chemistry type
   * @return the object
   */
  public BasecallingModelSelector withChemistryType(String chemistryType) {

    if (chemistryType == null || chemistryType.isBlank()) {
      return this;
    }

    this.chemistryTypeFilter = chemistryType.trim().toLowerCase();
    return this;
  }

  /**
   * Add a filter based on the translocation speed.
   * @param translocationSpeed the translocation speed
   * @return the object
   */
  public BasecallingModelSelector withTranslocationSpeed(
      String translocationSpeed) {

    if (translocationSpeed == null || translocationSpeed.isBlank()) {
      return this;
    }

    this.translocationSpeedFilter = translocationSpeed.trim().toLowerCase();

    try {

      Integer.parseInt(this.translocationSpeedFilter);
      this.translocationSpeedFilter += "bps";

    } catch (NumberFormatException e) {
    }

    return this;
  }

  /**
   * Add a filter based on the sampling frequency.
   * @param samplingFrequency the sampling frequency
   * @return the object
   */
  public BasecallingModelSelector withSamplingFrequency(
      String samplingFrequency) {

    if (samplingFrequency == null || samplingFrequency.isBlank()) {
      return this;
    }

    this.samplingFrequencyFilter = samplingFrequency.trim().toLowerCase();

    try {

      Integer.parseInt(this.samplingFrequencyFilter);
      this.samplingFrequencyFilter += "khz";

    } catch (NumberFormatException e) {
    }

    return this;
  }

  /**
   * Add a filter based on the model type.
   * @param modelType the model type
   * @return the object
   */
  public BasecallingModelSelector withModelType(String modelType) {

    if (modelType == null || modelType.isBlank()) {
      return this;
    }

    this.modelTypeFilter = modelType.trim().toLowerCase();
    return this;
  }

  /**
   * Add a filter based on the version of the model.
   * @param version the requested version of the model
   * @return the object
   */
  public BasecallingModelSelector withVersion(String version) {

    if (version == null || version.isBlank()) {
      return this;
    }

    this.versionFilter = version.trim().toLowerCase();
    return this;
  }

  /**
   * Add a filter based on the flowcell reference.
   * @param flowcell the flowcell reference
   * @return the object
   */
  public BasecallingModelSelector withFlowcellReference(String flowcell) {

    if (flowcell == null || flowcell.isBlank()) {
      return this;
    }

    this.poreTypeFilter = getPoreType(flowcell);
    return this;
  }

  /**
   * Add a filter based on the kit reference.
   * @param kit the kit reference
   * @return the object
   */
  public BasecallingModelSelector withKitReference(String kit) {

    if (kit == null || kit.isBlank()) {
      return this;
    }

    this.chemistryTypeFilter = getChemistryType(kit);
    return this;
  }

  /**
   * Add a filter that reject models that handle modifications.
   * @return the object
   */
  public BasecallingModelSelector withNoModification() {

    this.compatibleModificationsFilter = Collections.emptySet();
    return this;
  }

  /**
   * Add a filter that allow models that handle a modification.
   * @param modification required modification
   * @return the object
   */
  public BasecallingModelSelector withModification(String modification) {

    if (modification == null || modification.isBlank()) {
      return this;
    }

    if (this.compatibleModificationsFilter == null
        || this.compatibleModificationsFilter.isEmpty()) {
      this.compatibleModificationsFilter = new HashSet<>();
    }

    this.compatibleModificationsFilter.add(modification.trim().toLowerCase());
    return this;
  }

  //
  // Filtering
  //

  private Model filterModel() {

    Set<Model> result = new HashSet<>();

    for (Model m : this.models) {

      if (discard(this.analyteTypeFilter, m.analyteType)) {
        continue;
      }

      if (discard(this.poreTypeFilter, m.poreType)) {
        continue;
      }

      if (discard(this.chemistryTypeFilter, m.chemistryType)) {
        continue;
      }

      if (discard(this.translocationSpeedFilter, m.translocationSpeed)) {
        continue;
      }

      if (discard(this.samplingFrequencyFilter, m.samplingFrequency)) {
        continue;
      }

      if (discard(this.modelTypeFilter, m.modelType)) {
        continue;
      }

      if (discard(this.versionFilter, m.version)) {
        continue;
      }

      if (this.compatibleModificationsFilter != null) {

        if (this.compatibleModificationsFilter.isEmpty()
            && !m.compatibleModifications.isEmpty()) {
          continue;
        }

        for (String modif : this.compatibleModificationsFilter) {

          if (!m.compatibleModifications.contains(modif)) {
            continue;
          }
        }
      }

      result.add(m);
    }

    return selectLatestVersion(result);
  }

  /**
   * Select the right model.
   * @return the selected model
   */
  public Path select() {

    Model result = filterModel();

    if (result == null) {
      return null;
    }

    return new File(this.modelsPath.toFile(), result.name).toPath();
  }

  private static Model selectLatestVersion(Set<Model> models) {

    if (models.isEmpty()) {
      return null;
    }

    if (models.size() == 1) {
      return models.iterator().next();
    }

    Map<Version, Model> map = new HashMap<>();
    for (Model m : models) {

      String version = m.version.substring(1);
      map.put(new Version(version), m);
    }

    Version selected =
        Version.getMaximalVersion(new ArrayList<Version>(map.keySet()));
    return map.get(selected);
  }

  private static boolean discard(String filter, String value) {

    if (filter == null) {
      return false;
    }

    return !filter.equals(value);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param modelsPath path of the Dorado models
   */
  public BasecallingModelSelector(Path modelsPath) {

    requireNonNull(modelsPath);

    this.modelsPath = modelsPath;
    listModels(modelsPath);
  }

}
