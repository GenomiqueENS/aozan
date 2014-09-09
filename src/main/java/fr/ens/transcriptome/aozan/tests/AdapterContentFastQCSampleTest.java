package fr.ens.transcriptome.aozan.tests;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import uk.ac.babraham.FastQC.Modules.PerTileQualityScores;
import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a sample test for FastQC per tile sequence quality module.
 * @since 1.3
 * @author Sandrine Perrin
 * @author Laurent Jourdren
 */
public class AdapterContentFastQCSampleTest extends
    AbstractFastQCSampleTest {

  private static final String FASTQC_MODULE_NAME = new PerTileQualityScores()
      .name();

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  protected String getQCModuleName() {

    return FASTQC_MODULE_NAME;
  }

  @Override
  protected int getHTMLAnchorIndex() {

    return 10;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public AdapterContentFastQCSampleTest () {

    super("adaptercontent", "adapter content",
        "Adapter content");
  }

}

