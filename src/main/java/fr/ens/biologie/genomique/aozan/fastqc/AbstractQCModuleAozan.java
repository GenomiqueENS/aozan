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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.fastqc;

import java.io.IOException;

import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;

/**
 * This class replace normal class AbstracQCModule form
 * @author Sandrine Perrin
 * @since 1.3
 */
public abstract class AbstractQCModuleAozan extends AbstractQCModule {

  @Override
  protected void writeTable(final HTMLReportArchive report,
      final TableModel table) throws IOException, XMLStreamException {
    writeXhtmlTable(report, table);
    writeTextTable(report, table);
  }

  @Override
  protected void writeXhtmlTable(final HTMLReportArchive report,
      final TableModel table) throws IOException, XMLStreamException {
    final XMLStreamWriter w = report.xhtmlStream();
    w.writeStartElement("table");
    w.writeStartElement("thead");
    w.writeStartElement("tr");

    for (int c = 0; c < table.getColumnCount(); c++) {
      w.writeStartElement("th");
      w.writeCharacters(table.getColumnName(c));
      w.writeEndElement();
    }

    w.writeEndElement();// tr
    w.writeEndElement();// thead
    w.writeStartElement("tbody");

    for (int r = 0; r < table.getRowCount(); r++) {
      w.writeStartElement("tr");
      for (int c = 0; c < table.getColumnCount(); c++) {
        w.writeStartElement("td");

        final String text = String.valueOf(table.getValueAt(r, c)).trim();
        writeContentCell(w, text);

        w.writeEndElement();// td
      }
      w.writeEndElement();// tr
    }
    w.writeEndElement();// tbody
    w.writeEndElement();
  }

  @Override
  protected void writeTextTable(final HTMLReportArchive report,
      final TableModel table) throws IOException {

    final StringBuffer d = report.dataDocument();
    d.append('#');

    for (int c = 0; c < table.getColumnCount(); c++) {
      if (c != 0) {
        d.append('\t');
      }
      d.append(table.getColumnName(c));
    }

    d.append('\n');

    // Do the rows
    for (int r = 0; r < table.getRowCount(); r++) {
      for (int c = 0; c < table.getColumnCount(); c++) {
        if (c != 0) {
          d.append('\t');
        }
        // Remove tag html
        d.append(trimTagHtml(String.valueOf(table.getValueAt(r, c))));
      }
      d.append('\n');
    }
  }

  /**
   * Remove link tag html in text.
   * @param text text to modify
   * @return text with tag html
   */
  private String trimTagHtml(final String text) {

    final int startPos = text.indexOf("<a href");
    final int endPos = text.indexOf("</a>");

    String result = text;

    // Check link html
    if (startPos > 0 && endPos > startPos) {
      // Remove link tag
      final String s =
          text.substring(0, startPos - 2) + ". " + text.substring(endPos + 4);

      // Remove new line tag
      result = s.replaceAll("<br/>", "");

    }

    return result;
  }

  private void writeContentCell(final XMLStreamWriter w, final String text)
      throws XMLStreamException {

    // Check link html
    final int startPos = text.indexOf("<a href");
    final int endPos = text.indexOf("</a>");

    // Create link element
    if (startPos > 0 && endPos > startPos) {

      String s = text.substring(0, startPos - 1);
      if (s != null && s.length() > 0) {
        w.writeCharacters(s.trim());
      }

      // Create link
      w.writeStartElement("a");
      // Add url
      final int hrefEndPos = text.indexOf("target");
      s = text.substring(startPos + 9, hrefEndPos - 2);
      w.writeAttribute("href", s.trim());
      w.writeAttribute("target", "_blank");

      // Add text link
      final int tagAEndPos = text.indexOf('>');
      s = text.substring(tagAEndPos + 1, endPos);
      w.writeCharacters(s.trim());

      w.writeEndElement(); // a
      w.writeEmptyElement("br");

      // Add last text
      s = text.substring(endPos + 4, text.length());
      // remove tab br
      final String newS = s.replaceAll("<br/>", "");
      w.writeCharacters(newS.trim());

    } else {
      // Add classic text
      w.writeCharacters(text);
    }

  }
}
