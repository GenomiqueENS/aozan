package fr.ens.biologie.genomique.aozan;

import static org.junit.Assert.*;

import org.junit.Test;

import fr.ens.biologie.genomique.aozan.aozan3.IlluminaUtils;

public class IlluminaUtilsTest {

  @Test
  public void testCheckRunId() {

    assertFalse(IlluminaUtils.checkRunId(""));

    assertTrue(IlluminaUtils.checkRunId("110915_SN501_0080_AB0A07ABXX"));
    assertTrue(IlluminaUtils.checkRunId("110502_SNL110_0023_AB0AE5ABXX"));
    assertTrue(IlluminaUtils.checkRunId("150327_NB500892_0001_AH2LCVAFXX"));
    assertTrue(IlluminaUtils.checkRunId("161115_SN953_0252_AC5PYKACXX"));
    assertTrue(IlluminaUtils.checkRunId("160304_ST-J00115_0008_AH5Y7NBBXX"));
    assertTrue(IlluminaUtils.checkRunId("170525_M01795_0459_000000000-B9JLP"));
    assertTrue(IlluminaUtils.checkRunId("170608_7001326F_0098_AHKVL5BCXY"));
    assertTrue(IlluminaUtils.checkRunId("210628_VH00567_1_AAAGLG7HV"));
  }

}
