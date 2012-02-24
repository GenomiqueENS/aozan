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

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.io.FileFilter;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.DesignCollector;
import fr.ens.transcriptome.aozan.collectors.FastQCCollector;
import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.transcriptome.aozan.collectors.PhasingCollector;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.tests.ClusterDensityLaneTest;
import fr.ens.transcriptome.aozan.tests.ErrorRate100CycleLaneTest;
import fr.ens.transcriptome.aozan.tests.ErrorRate35CycleLaneTest;
import fr.ens.transcriptome.aozan.tests.ErrorRate75CycleLaneTest;
import fr.ens.transcriptome.aozan.tests.ErrorRateLaneTest;
import fr.ens.transcriptome.aozan.tests.FirstCycleIntensityPFLaneTest;
import fr.ens.transcriptome.aozan.tests.LaneTest;
import fr.ens.transcriptome.aozan.tests.MeanQualityScoreSampleTest;
import fr.ens.transcriptome.aozan.tests.PFClustersLaneTest;
import fr.ens.transcriptome.aozan.tests.PFClustersPercentLaneTest;
import fr.ens.transcriptome.aozan.tests.PFClustersSampleTest;
import fr.ens.transcriptome.aozan.tests.PercentAlignLaneTest;
import fr.ens.transcriptome.aozan.tests.PercentCycle20IntensityLaneTest;
import fr.ens.transcriptome.aozan.tests.PercentInLaneSampleTest;
import fr.ens.transcriptome.aozan.tests.PercentPFSampleTest;
import fr.ens.transcriptome.aozan.tests.PercentQ30SampleTest;
import fr.ens.transcriptome.aozan.tests.PhasingPrePhasingLaneTest;
import fr.ens.transcriptome.aozan.tests.RawClustersLaneTest;
import fr.ens.transcriptome.aozan.tests.RawClustersSampleTest;
import fr.ens.transcriptome.aozan.tests.SampleTest;

public class QCDemo {

  private static final void processRun(final String bclDir,
      final String fastqDir, final String runId) throws IOException,
      AozanException {

    final File RTAOutputDir = new File(bclDir, runId);
    final File casavaOutputDir = new File(fastqDir, runId);

    File[] designFiles = casavaOutputDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {

        return name.endsWith(".csv");
      }
    });

    final File casavaDesignFile = designFiles[0];

    // Define the collectors
    final List<Collector> collectors =
        Lists.newArrayList(new RunInfoCollector(), new ReadCollector(),
            new DesignCollector(), new FlowcellDemuxSummaryCollector(),
            new PhasingCollector(), new FastQCCollector());

    // Create the run data object
    final RunData data =
        new RunDataGenerator(RTAOutputDir, casavaDesignFile, casavaOutputDir,
            collectors).collect();

    // Print the content of the run data object
    data.print();

    // Define the read tests
    final List<LaneTest> laneTests = Lists.newArrayList();
    laneTests.add(new RawClustersLaneTest());
    laneTests.add(new PFClustersLaneTest());
    laneTests.add(new PFClustersPercentLaneTest());
    laneTests.add(new ClusterDensityLaneTest());
    laneTests.add(new PercentAlignLaneTest());
    laneTests.add(new ErrorRateLaneTest());
    laneTests.add(new ErrorRate35CycleLaneTest());
    laneTests.add(new ErrorRate75CycleLaneTest());
    laneTests.add(new ErrorRate100CycleLaneTest());
    laneTests.add(new FirstCycleIntensityPFLaneTest());
    laneTests.add(new PercentCycle20IntensityLaneTest());
    laneTests.add(new PhasingPrePhasingLaneTest());

    // Define the sample tests
    final List<SampleTest> sampleTests = Lists.newArrayList();
    sampleTests.add(new RawClustersSampleTest());
    sampleTests.add(new PFClustersSampleTest());
    sampleTests.add(new PercentPFSampleTest());
    sampleTests.add(new PercentInLaneSampleTest());
    sampleTests.add(new PercentQ30SampleTest());
    sampleTests.add(new MeanQualityScoreSampleTest());

    // Create the report
    final QCReport report = new QCReport(data, laneTests, sampleTests);
    // System.out.println(report.toXML());

    Writer writer =
        new FileWriter(new File("/home/jourdren/qc-" + runId + ".xml"));
    writer.write(report.toXML());
    writer.close();

    writer = new FileWriter(new File("/home/jourdren/qc-" + runId + ".html"));
    writer.write(report.export(QCDemo.class
        .getResourceAsStream("/files/aozan.xsl")));
    writer.close();

  }

  public static final void main(String[] args) throws AozanException,
      IOException {

    Locale.setDefault(Locale.US);

    final String bclDir = "/home/jourdren/shares-net/sequencages/bcl";
    final String fastqDir = "/home/jourdren/shares-net/sequencages/fastq";

    final String runId = "120124_SNL110_0036_AD0DM3ABXX";
    // final String runId = "120210_SNL110_0037_AC0BE6ACXX";

    processRun(bclDir, fastqDir, runId);
    System.exit(0);

    final File[] runIdsDir = new File(fastqDir).listFiles(new FileFilter() {

      @Override
      public boolean accept(final File arg0) {

        return arg0.isDirectory();
      }
    });

    for (File runIdDir : runIdsDir) {
      if (!runIdDir.getName().contains("0024")
          && !runIdDir.getName().contains("0023"))
        processRun(bclDir, fastqDir, runIdDir.getName());
    }

  }
}
