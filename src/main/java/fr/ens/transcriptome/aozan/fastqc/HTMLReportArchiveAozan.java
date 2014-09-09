/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.fastqc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import fr.ens.transcriptome.aozan.Globals;
import uk.ac.babraham.FastQC.FastQCApplication;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;

/**
 * Source FastQC version 0.10.1, modify to provide access to file in fastqc jar
 * with the same code used in version fastqc 0.10.0. This class calls instead of
 * the original after modification of bytecode. Copyright 2010-11 Simon Andrews
 * @since 1.1
 */
public class HTMLReportArchiveAozan extends HTMLReportArchive {

  private final StringBuffer html = new StringBuffer();
  private final StringBuffer data = new StringBuffer();
  private QCModule[] modules;
  private ZipOutputStream zip;
  private SequenceFile sequenceFile;
  private final byte[] buffer = new byte[1024];
  private final File file;

  private void unzipZipFile(File file) throws IOException {
    ZipFile zipFile = new ZipFile(file);
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    int size;
    byte[] buffer = new byte[1024];

    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();

      if (entry.isDirectory()) {
        File dir = new File(file.getParent() + "/" + entry.getName());
        if (dir.exists() && dir.isDirectory())
          continue; // Don't need to do anything
        if (dir.exists() && !dir.isDirectory())
          throw new IOException("File exists with dir name " + dir.getName());
        if (!dir.mkdir())
          throw new IOException("Failed to make dir for " + dir.getName());
        continue;
      }

      BufferedInputStream bis =
          new BufferedInputStream(zipFile.getInputStream(entry));
      BufferedOutputStream bos =
          new BufferedOutputStream(new FileOutputStream(file.getParent()
              + "/" + entry.getName()), buffer.length);
      while ((size = bis.read(buffer, 0, buffer.length)) != -1) {
        bos.write(buffer, 0, size);
      }
      bos.flush();
      bos.close();
      bis.close();
    }
  }

  public StringBuffer htmlDocument() {
    return html;
  }

  public StringBuffer dataDocument() {
    return data;
  }

  public String folderName() {
    return file.getName().replaceAll(".zip$", "");
  }

  public ZipOutputStream zipFile() {
    return zip;
  }

  private void startDocument() throws IOException {

    // Just put the fastQC version at the start of the text report
    data.append("##FastQC\t");
    data.append(FastQCApplication.VERSION);
    data.append("\n");

    // VERSION FastQC 0.10.1
    // Add in the icon files for pass/fail/warn
    // File templatesDir =
    // new File(
    // URLDecoder.decode(ClassLoader.getSystemResource("Templates/Icons")
    // .getFile(), "UTF-8"));
    // String[] names = templatesDir.list();
    // for (int n = 0; n < names.length; n++) {
    // if (names[n].toLowerCase().endsWith(".png")
    // || names[n].toLowerCase().endsWith(".jpg")
    // || names[n].toLowerCase().endsWith(".jpeg")) {
    // zip.putNextEntry(new ZipEntry(folderName() + "/Icons/" + names[n]));
    // FileInputStream fileIn =
    // new FileInputStream(new File(URLDecoder.decode(ClassLoader
    // .getSystemResource("Templates/Icons/" + names[n]).getFile(),
    // "UTF-8")));
    // int len;
    // while ((len = fileIn.read(buffer)) > 0) {
    // zip.write(buffer, 0, len);
    // }
    // fileIn.close();
    // }
    // }

    // VERSION FastQC 0.10.0
    // Add in the icon files for pass/fail/warn
    for (final String ressource : listResourceDirectory("Templates/Icons")) {

      final String name = new File(ressource).getName();

      if (name.toLowerCase().endsWith(".png")
          || name.toLowerCase().endsWith(".jpg")
          || name.toLowerCase().endsWith(".jpeg")) {
        zip.putNextEntry(new ZipEntry(folderName() + "/Icons/" + name));

        InputStream fileIn =
            ClassLoader.getSystemResourceAsStream("Templates/Icons/" + name);
        int len;
        while ((len = fileIn.read(buffer)) > 0) {
          zip.write(buffer, 0, len);
        }
        fileIn.close();
      }
    }

    SimpleDateFormat df = new SimpleDateFormat("EEE d MMM yyyy");
    addTemplate(sequenceFile.name(), df.format(new Date()));

    html.append("<h2>Summary</h2>\n<ul>\n");

    StringBuffer summaryText = new StringBuffer();

    for (int m = 0; m < modules.length; m++) {
      html.append("<li>");
      html.append("<img src=\"");
      if (modules[m].raisesError()) {
        html.append("Icons/error.png\" alt=\"[FAIL]\"> ");
        summaryText.append("FAIL");
      } else if (modules[m].raisesWarning()) {
        html.append("Icons/warning.png\" alt=\"[WARNING]\"> ");
        summaryText.append("WARN");
      } else {
        html.append("Icons/tick.png\" alt=\"[PASS]\"> ");
        summaryText.append("PASS");
      }
      summaryText.append("\t");
      summaryText.append(modules[m].name());
      summaryText.append("\t");
      summaryText.append(sequenceFile.name());
      summaryText.append(System.getProperty("line.separator"));

      html.append("<a href=\"#M");
      html.append(m);
      html.append("\">");
      html.append(modules[m].name());
      html.append("</a></li>\n");

    }
    html.append("</ul>\n</div>\n<div class=\"main\">\n");

    zip.putNextEntry(new ZipEntry(folderName() + "/summary.txt"));
    zip.write(summaryText.toString().getBytes());

  }

  private static List<String> listResourceDirectory(
      final String resourceDirectory) {

    if (resourceDirectory == null) {
      return null;
    }

    final List<String> result = new ArrayList<String>();

    try {

      // Modification with 0.10.0 : call a class present in file jar
      final File dirOrJar =
          new File(uk.ac.babraham.FastQC.FastQCApplication.class
              .getProtectionDomain().getCodeSource().getLocation().toURI());

      if (dirOrJar.isFile()) {

        // It is a jar file
        // Open the jar file
        final JarInputStream jis =
            new JarInputStream(new FileInputStream(dirOrJar));

        // Get the list of all the entries
        JarEntry jarEntry;
        do {
          jarEntry = jis.getNextJarEntry();

          // Filter entries to find files in the resource directory
          if (jarEntry != null
              && !jarEntry.getName().equals(resourceDirectory)
              && jarEntry.getName().startsWith(resourceDirectory))
            result.add(jarEntry.getName());

        } while (jarEntry != null);

        jis.close();
        return result;
      }

      // It is a directory
      // List the files of the directory
      for (File f : dirOrJar.listFiles())
        result.add(f.getAbsolutePath());

      return result;

    } catch (IOException e) {
      return null;
    } catch (URISyntaxException e) {
      return null;
    } catch (NullPointerException e) {
      return null;
    }

  }

  private void closeDocument() {

    html.append("</div><div class=\"footer\">Produced by <a href=\"http://www.bioinformatics.babraham.ac.uk/projects/fastqc/\">FastQC</a> (version ");
    html.append(FastQCApplication.VERSION);
    html.append(")</div>\n");

    html.append("</body></html>");
  }

  private void addTemplate(String filename, String date) throws IOException {

    // VERSION FastQC 0.10.1
    // BufferedReader br =
    // new BufferedReader(new FileReader(new File(URLDecoder.decode(
    // ClassLoader.getSystemResource("Templates/header_template.html")
    // .getFile(), "UTF-8"))));

    // VERSION FastQC 0.10.0
    BufferedReader br =
        new BufferedReader(
            new InputStreamReader(ClassLoader
                .getSystemResourceAsStream("Templates/header_template.html"),
                Globals.DEFAULT_FILE_ENCODING));

    String line;
    while ((line = br.readLine()) != null) {

      line = line.replaceAll("@@FILENAME@@", filename);
      line = line.replaceAll("@@DATE@@", date);

      html.append(line);
      html.append("\n");
    }

    br.close();

  }

  //
  // Constructor
  //

  public HTMLReportArchiveAozan(SequenceFile sequenceFile, QCModule[] modules,
      File file) throws IOException, XMLStreamException {

    super(null, null, null);

    this.sequenceFile = sequenceFile;
    this.modules = Arrays.copyOf(modules, modules.length);

    this.file = file;
    zip = new ZipOutputStream(new FileOutputStream(file));
    zip.putNextEntry(new ZipEntry(folderName() + "/"));
    zip.putNextEntry(new ZipEntry(folderName() + "/Icons/"));
    zip.putNextEntry(new ZipEntry(folderName() + "/Images/"));
    startDocument();
    for (int m = 0; m < modules.length; m++) {
      html.append("<div class=\"module\"><h2 id=\"M");
      html.append(m);
      html.append("\">");

      // Add an icon before the module name
      if (modules[m].raisesError()) {
        html.append("<img src=\"Icons/error.png\" alt=\"[FAIL]\"> ");
      } else if (modules[m].raisesWarning()) {
        html.append("<img src=\"Icons/warning.png\" alt=\"[WARN]\"> ");
      } else {
        html.append("<img src=\"Icons/tick.png\" alt=\"[OK]\"> ");
      }

      html.append(modules[m].name());
      data.append(">>");
      data.append(modules[m].name());
      data.append("\t");
      if (modules[m].raisesError()) {
        data.append("fail");
      } else if (modules[m].raisesWarning()) {
        data.append("warn");
      } else {
        data.append("pass");
      }
      data.append("\n");
      html.append("</h2>\n");
      modules[m].makeReport(this);
      data.append(">>END_MODULE\n");

      html.append("</div>\n");
    }
    closeDocument();

    zip.putNextEntry(new ZipEntry(folderName() + "/fastqc_report.html"));
    zip.write(html.toString().getBytes());
    zip.closeEntry();
    zip.putNextEntry(new ZipEntry(folderName() + "/fastqc_data.txt"));
    zip.write(data.toString().getBytes());
    zip.closeEntry();
    zip.close();

    if (System.getProperty("fastqc.unzip").equals("true")) {
      unzipZipFile(file);
    }

  }
}
