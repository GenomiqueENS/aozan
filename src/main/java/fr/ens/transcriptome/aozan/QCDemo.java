/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class QCDemo {

  private static List<String> getRunIds(final String fastqDir) {

    final File[] runIdsDir = new File(fastqDir).listFiles(new FileFilter() {

      @Override
      public boolean accept(final File arg0) {

        return arg0.isDirectory();
      }
    });

    List<String> result = Lists.newArrayList();

    if (result != null)
      for (File dir : runIdsDir)
        result.add(dir.getName());

    return result;
  }

  public static final void main(String[] args) throws AozanException,
      IOException {

    Locale.setDefault(Locale.US);

    final String bclDir = "/home/jourdren/shares-net/sequencages/bcl";
    final String fastqDir = "/home/jourdren/shares-net/sequencages/fastq";

    final Map<String, String> properties = Maps.newHashMap();

    // Lanes tests
    properties.put("qc.test.rawclusters.enable", "true");
    properties.put("qc.test.pfclusters.enable", "true");
    properties.put("qc.test.pfclusterspercent.enable", "true");
    properties.put("qc.test.clusterdensity.enable", "true");
    properties.put("qc.test.percentalign.enable", "true");
    properties.put("qc.test.errorrate.enable", "true");
    properties.put("qc.test.errorrate35cycle.enable", "true");
    properties.put("qc.test.errorrate75cycle.enable", "true");
    properties.put("qc.test.errorrate100cycle.enable", "true");
    properties.put("qc.test.firstcycleintensity.enable", "true");
    properties.put("qc.test.percentintensitycycle20.enable", "true");
    properties.put("qc.test.phasingprephasing.enable", "true");

    // Sample tests
    properties.put("qc.test.rawclusterssamples.enable", "true");
    properties.put("qc.test.pfclusterssamples.enable", "true");
    properties.put("qc.test.percentpfsample.enable", "true");
    properties.put("qc.test.percentinlanesample.enable", "true");
    properties.put("qc.test.percentq30.enable", "true");
    properties.put("qc.test.meanqualityscore.enable", "true");

    // final List<String> runIds =
    // newArrayList("120124_SNL110_0036_AD0DM3ABXX");
    // final List<String> runIds =
    // newArrayList("120210_SNL110_0037_AC0BE6ACXX");
    final List<String> runIds = newArrayList("120301_SNL110_0038_AD0EJRABXX");
    // final List<String> runIds = getRunIds(fastqDir);

    // Process all runs
    final QC qc = new QC(properties);

    for (final String runId : runIds) {
      if (!runId.contains("0024") && !runId.contains("0023")) {

        // Output xml file
        final File reportXmlFile =
            new File("/home/jourdren/qc-" + runId + ".xml");

        // Output html file
        final File reportHtmlFile =
            new File("/home/jourdren/qc-" + runId + ".html");

        // XSL stylesheet
        final InputStream xslIs =
            QCDemo.class.getResourceAsStream("/files/aozan.xsl");

        // Compute report
        final QCReport report = qc.computeReport(bclDir, fastqDir, runId);

        // Save report data
        qc.writeReport(report, null, reportXmlFile);

        // Save HTML report
        qc.writeReport(report, xslIs, reportHtmlFile);
      }
    }

  }
}
