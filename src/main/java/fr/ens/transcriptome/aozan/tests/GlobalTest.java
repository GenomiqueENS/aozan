package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;

/**
 * This interface define a test on lane.
 * @since 1.x
 * @author Laurent Jourdren
 */
public interface GlobalTest extends AozanTest {

  /**
   * Do a test.
   * @param data result object
   * @return a TestResult object with the result of the test
   */
  public TestResult test(RunData data);

}
