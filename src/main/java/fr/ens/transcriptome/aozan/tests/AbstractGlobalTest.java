package fr.ens.transcriptome.aozan.tests;

/**
 * This class define a abstract Global test.
 * @since 1.x
 * @author Laurent Jourdren
 */
public abstract class AbstractGlobalTest implements GlobalTest {

  private final String name;
  private final String description;
  private final String columnName;
  private final String unit;

  //
  // Getters
  //

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getDescription() {

    return this.description;
  }

  @Override
  public String getColumnName() {

    return this.columnName;
  }

  @Override
  public String getUnit() {

    return this.unit;
  }

  //
  // Other methods
  //

  @Override
  public void init() {
  }

  //
  // Constructor
  //

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   */
  protected AbstractGlobalTest(final String name, final String description,
      final String columnName) {

    this(name, description, columnName, "");
  }

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   * @param unit unit of the test
   */
  protected AbstractGlobalTest(final String name, final String description,
      final String columnName, final String unit) {

    this.name = name;
    this.description = description;
    this.columnName = columnName;
    this.unit = unit;
  }

}
