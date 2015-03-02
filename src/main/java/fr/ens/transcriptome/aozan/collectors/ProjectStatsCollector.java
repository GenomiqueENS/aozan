package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.util.XMLUtilsWriter;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;

public class ProjectStatsCollector implements Collector {

  /** Collector name. */
  public static final String COLLECTOR_NAME = "projectstats";

  public static final String COLLECTOR_PREFIX = "projectstats.";

  private String reportDir;

  private File fastqscreenXSLFile;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector.
   * @return list of names collector
   */
  @Override
  public List<String> getCollectorsNamesRequiered() {

    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME,
        UndeterminedIndexesCollector.COLLECTOR_NAME,
        FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(Properties properties) {
    this.reportDir = properties.getProperty(QC.QC_OUTPUT_DIR);

    try {
      final String filename =
          properties
              .getProperty(Settings.QC_CONF_FASTQSCREEN_PROJECT_XSL_FILE_KEY);
      if (new File(filename).exists()) {
        this.fastqscreenXSLFile = new File(filename);
      }
    } catch (final Exception e) {
      // Call default xsl file
      this.fastqscreenXSLFile = null;
    }
  }

  @Override
  public void clear() {

  }

  @Override
  public void collect(RunData data) throws AozanException {

    // Parse FastqSample to build list Project
    final List<ProjectStat> projects = extractProjects(data);

    // Collect projects statistics in rundata
    for (ProjectStat project : projects) {
      data.put(project.createRunDataProject());
    }

    try {
      // Build FastqScreen project report html
      createProjectsReport(projects);
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  private void createProjectsReport(final List<ProjectStat> projects)
      throws AozanException, IOException {

    for (ProjectStat p : projects) {
      final FastqScreenProjectReport fpr = new FastqScreenProjectReport(p);

      fpr.createReport(p.getReportHtmlFile());

    }

  }

  private List<ProjectStat> extractProjects(RunData data) throws AozanException {

    final int laneCount = data.getLaneCount();
    final Map<String, ProjectStat> projects = initMap(data);

    // Add projects name
    for (int lane = 1; lane <= laneCount; lane++) {

      // Parse all samples in lane
      for (String sampleName : data.getSamplesNameListInLane(lane)) {

        // Extract project name related to sample name
        final String projectName = data.getProjectSample(lane, sampleName);

        // Save new sample in related project
        projects.get(projectName).addSample(lane, sampleName);
      }
    }

    final List<ProjectStat> projectsSorted =
        new ArrayList<ProjectStatsCollector.ProjectStat>(projects.values());

    return Collections.unmodifiableList(projectsSorted);
  }

  private Map<String, ProjectStat> initMap(RunData data) throws AozanException {

    final Map<String, ProjectStat> projects =
        new HashMap<String, ProjectStatsCollector.ProjectStat>();

    // Add projects name
    final List<String> projectsName = data.getProjectsNameList();

    for (String projectName : projectsName) {
      projects.put(projectName, new ProjectStat(data, projectName));
    }

    return Collections.unmodifiableMap(projects);
  }

  //
  // Internal class
  //

  final class ProjectStat {

    private static final String DEFAULT_GENOME = "No genome.";

    private static final int READ = 1;

    private final RunData data;
    private final String projectName;
    private final List<File> fastqscreenReportSamples;
    private final Set<String> genomes;
    private final List<String> samples;
    private final Set<Integer> lanes;

    private int rawClusterSum = 0;
    private int pfClusterSum = 0;
    private int sampleCount = 0;
    private boolean isIndexed;
    private int rawClusterRecoverySum = 0;
    private int pfClusterRecoverySum = 0;
    private File projectDir;

    public RunData createRunDataProject() {

      final RunData data = new RunData();
      data.put(getPrefixRunData() + ".lanes", Joiner.on(",").join(this.lanes));
      data.put(getPrefixRunData() + ".genomes.ref",
          Joiner.on(",").join(getGenomes()));
      data.put(getPrefixRunData() + ".samples.count", samples.size());
      data.put(getPrefixRunData() + ".isindexed", isIndexed);
      data.put(getPrefixRunData() + ".raw.cluster.count", rawClusterSum);
      data.put(getPrefixRunData() + ".pf.cluster.count", pfClusterSum);
      data.put(getPrefixRunData() + ".raw.cluster.recovery.count",
          rawClusterRecoverySum);
      data.put(getPrefixRunData() + ".pf.cluster.recovery.count",
          pfClusterRecoverySum);

      return data;
    }

    private Set<String> getGenomes() {

      if (this.genomes.isEmpty())
        return Collections.singleton(DEFAULT_GENOME);

      return this.genomes;
    }

    public File getReportHtmlFile() {

      return new File(reportDir + "/Project_" + this.projectName,
          this.projectName + "-fastqscreen.html");
    }

    private String getPrefixRunData() {
      return COLLECTOR_PREFIX + projectName;
    }

    public List<String> getSamples() {
      return samples;
    }

    public void addSample(final int lane, final String sample) {
      this.lanes.add(lane);
      this.sampleCount++;

      isIndexed = this.data.isLaneIndexed(lane);

      rawClusterSum += this.data.getSampleRawClusterCount(lane, READ, sample);
      pfClusterSum += this.data.getSamplePFClusterCount(lane, READ, sample);

      rawClusterRecoverySum +=
          this.data.getSampleRawClusterRecoveryCount(lane, sample);

      pfClusterRecoverySum +=
          this.data.getSamplePFClusterRecoveryCount(lane, sample);

      samples.add(sample);

      // Extract from samplesheet file
      genomes.add(data.getSampleGenome(lane, sample));

    }

    private List<File> extractFastqscreenReport() throws AozanException {

      if (!projectDir.exists())
        throw new AozanException("Project directory does not exist "
            + projectDir.getAbsolutePath());

      // Exrtact in project directy all fastqscreen report xml
      final List<File> reports =
          Arrays.asList(projectDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
              return pathname.length() > 0
                  && pathname.getName().endsWith("-fastqscreen.xml");
            }
          }));

      // Sort by filename
      Collections.sort(reports);

      return Collections.unmodifiableList(reports);
    }

    //
    // Getter
    //

    public List<File> getFastqScreenReport() {

      if (this.fastqscreenReportSamples.size() != this.sampleCount)
        throw new EoulsanRuntimeException("In project "
            + projectName + " samples count " + sampleCount
            + " incompatible with fastqscreen report found "
            + this.fastqscreenReportSamples.size());

      return Collections.unmodifiableList(this.fastqscreenReportSamples);
    }

    @Override
    public String toString() {
      return "ProjectStat [data="
          + data + ", projectName=" + projectName
          + ", fastqscreenReportSamples=" + fastqscreenReportSamples
          + ", genomes=" + genomes + ", samples=" + samples
          + ", rawClusterSum=" + rawClusterSum + ", pfClusterSum="
          + pfClusterSum + ", sampleCount=" + sampleCount + ", isIndexed="
          + isIndexed + ", rawClusterRecoverySum=" + rawClusterRecoverySum
          + ", pfClusterRecoverySum=" + pfClusterRecoverySum + ", projectDir="
          + projectDir + "]";
    }

    //
    // Constructor
    //
    public ProjectStat(final RunData runData, final String projectName)
        throws AozanException {
      this.data = runData;
      this.projectName = projectName;

      this.genomes = new LinkedHashSet<>();
      this.lanes = new LinkedHashSet<>();
      this.samples = new ArrayList<>();

      this.projectDir = new File(reportDir + "/Project_" + this.projectName);

      this.fastqscreenReportSamples = extractFastqscreenReport();

    }
  }

  //
  // Internal class
  //
  class FastqScreenProjectReport {

    private final ProjectStat projectStat;

    FastqScreenProjectReport(final ProjectStat project)
        throws FileNotFoundException {

      projectStat = project;
    }

    public void createReport(final File reportHtml) throws AozanException,
        IOException {

      // Call stylesheet file for report
      InputStream is = null;
      if (fastqscreenXSLFile == null) {
        is =
            this.getClass().getResourceAsStream(
                Globals.EMBEDDED_FASTQSCREEN_PROJECT_XSL);
      } else {
        is = new FileInputStream(fastqscreenXSLFile);
      }

      final Document doc = buildDoc();

      XMLUtilsWriter.createXMLFile(doc);

      XMLUtilsWriter.createHTMLFileFromXSL(doc, is, reportHtml);
    }

    private Document buildDoc() throws AozanException {

      boolean runDataAdded = false;

      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

      DocumentBuilder docBuilder = null;
      Document doc = null;

      try {
        docBuilder = dbfac.newDocumentBuilder();
        doc = docBuilder.newDocument();

      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      }

      // Create the root element and add it to the document
      Element root = doc.createElement("ReportFastqScreen");
      root.setAttribute("formatversion", "1.0");
      doc.appendChild(root);

      final Element project = doc.createElement("project");
      root.appendChild(project);

      for (File fqsxml : projectStat.getFastqScreenReport()) {
        final Document srcDoc = buildDom(fqsxml);

        if (!runDataAdded) {
          // Extract run data from first xml file
          extractRunData(doc, root, srcDoc);
          runDataAdded = true;
        }

        final String name = fqsxml.getName().substring(0, 10);

        Element sample = doc.createElement("sample");
        sample.setAttribute("name", name);
        sample.setAttribute("path", fqsxml.getAbsolutePath());
        project.appendChild(sample);

        extractSampleData(doc, sample, srcDoc);
      }

      return doc;
    }

    private void extractRunData(final Document finalDoc, final Element root,
        final Document doc) {

      final List<String> runDataTag = Lists.newArrayList();
      runDataTag.add("GeneratorName");
      runDataTag.add("GeneratorVersion");
      runDataTag.add("GeneratorWebsite");
      runDataTag.add("GeneratorRevision");
      runDataTag.add("RunId");
      runDataTag.add("RunDate");
      runDataTag.add("FlowcellId");
      runDataTag.add("InstrumentSN");
      runDataTag.add("InstrumentRunNumber");
      runDataTag.add("ReportDate");
      runDataTag.add("projectName");
      runDataTag.add("genomeSample");

      final Element srcRoot = doc.getDocumentElement();
      for (final String tagName : runDataTag) {

        final NodeList childs = srcRoot.getElementsByTagName(tagName);

        final Element elem = (Element) childs.item(0);

        Node newElem = finalDoc.importNode(elem, true);
        root.appendChild(newElem);
      }

    }

    private Document buildDom(final File fqsxml) throws AozanException {

      Document doc = null;

      try {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();

        doc = dBuilder.parse(fqsxml);

      } catch (ParserConfigurationException | SAXException | IOException e) {
        throw new AozanException(e);
      }

      // optional, but recommended
      // read this -
      // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work

      if (doc == null) {
        throw new AozanException("Fail to initialisation document with "
            + fqsxml.getAbsolutePath());
      }

      doc.getDocumentElement().normalize();

      return doc;
    }

    private void extractSampleData(final Document finalDoc,
        final Element sample, final Document doc) {

      final List<String> sampleDataTag = Lists.newArrayList();
      sampleDataTag.add("sampleName");
      sampleDataTag.add("descriptionSample");
      sampleDataTag.add("Report");

      final Element srcRoot = doc.getDocumentElement();

      for (final String tagName : sampleDataTag) {
        final NodeList childs = srcRoot.getElementsByTagName(tagName);

        final Element elem = (Element) childs.item(0);

        Node newElem = finalDoc.importNode(elem, true);
        sample.appendChild(newElem);

      }
    }

  }
}
