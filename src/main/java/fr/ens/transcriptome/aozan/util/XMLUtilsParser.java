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

package fr.ens.transcriptome.aozan.util;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * The class define utility methods to extract information to a xml with the
 * good type.
 * @since 1.2
 * @author Sandrine Perrin
 */
public final class XMLUtilsParser {

  /**
   * Return the first value at a specific index corresponding to the tag name in
   * the element.
   * @param e root element
   * @param tagName tag name to extract
   * @param index value
   * @return value of tag
   */
  public static Integer extractValueToInt(final Element e,
      final String tagName, final int index) {
    List<Element> elements = XMLUtils.getElementsByTagName(e, tagName);

    if (index >= elements.size())
      return null;

    Element firstNode = elements.get(index);

    try {
      return Integer.parseInt(firstNode.getTextContent());

    } catch (NumberFormatException n) {
      return null;
    }
  }

  public static Integer extractFirstValueToInt(final Element e,
      final String tagName) {
    return extractValueToInt(e, tagName, 0);
  }

  /**
   * Return the first value at a specific index corresponding to the tag name in
   * the element.
   * @param e root element
   * @param tagName tag name to extract
   * @param index value
   * @return value of tag
   */
  public static Double extractValueToDouble(final Element e,
      final String tagName, final int index) {
    List<Element> elements = XMLUtils.getElementsByTagName(e, tagName);

    if (index >= elements.size())
      return null;

    Element firstNode = elements.get(index);
    try {
      return Double.parseDouble(firstNode.getTextContent());
    } catch (NumberFormatException n) {
      return null;
    }
  }

  public static Double extractFirstValueToDouble(final Element e,
      final String tagName) {
    return extractValueToDouble(e, tagName, 0);
  }

  /**
   * Return the first value at a specific index corresponding to the tag name in
   * the element.
   * @param e root element
   * @param tagName tag name to extract
   * @param index value
   * @return value of tag
   */
  public static String extractValueToString(final Element e,
      final String tagName, final int index) {
    List<Element> elements = XMLUtils.getElementsByTagName(e, tagName);

    if (index >= elements.size())
      return null;

    Element firstNode = elements.get(index);

    return firstNode.getTextContent();
  }

  public static String extractFirstValueToString(final Element e,
      final String tagName) {
    return extractValueToString(e, tagName, 0);
  }

  /**
   * Return the first value at a specific index corresponding to the tag name in
   * the document.
   * @param e root element
   * @param tagName tag name to extract
   * @param index value
   * @return value of tag
   */
  public static Integer extractValueToInt(final Document doc,
      final String tagName, final int index) {
    List<Element> elements = XMLUtils.getElementsByTagName(doc, tagName);
    if (index >= elements.size())
      return null;

    Element firstNode = elements.get(index);

    try {
      return Integer.parseInt(firstNode.getTextContent());

    } catch (NumberFormatException n) {
      return null;
    }
  }

  public static Integer extractFirstValueToInt(final Document doc,
      final String tagName) {
    return extractValueToInt(doc, tagName, 0);
  }

  /**
   * Return the first value at a specific index corresponding to the tag name in
   * the document.
   * @param e root element
   * @param tagName tag name to extract
   * @param index value
   * @return value of tag
   */
  public static Double extractValueToDouble(final Document doc,
      final String tagName, final int index) {
    List<Element> elements = XMLUtils.getElementsByTagName(doc, tagName);

    if (index >= elements.size())
      return null;

    Element firstNode = elements.get(index);
    try {
      return Double.parseDouble(firstNode.getTextContent());
    } catch (NumberFormatException n) {
      return null;
    }
  }

  public static Double extractFirstValueToDouble(final Document doc,
      final String tagName) {
    return extractValueToDouble(doc, tagName, 0);
  }

  /**
   * Return the first value at a specific index corresponding to the tag name in
   * the document.
   * @param e root element
   * @param tagName tag name to extract
   * @param index value
   * @return value of tag
   */
  public static String extractValueToString(final Document doc,
      final String tagName, final int index) {
    List<Element> elements = XMLUtils.getElementsByTagName(doc, tagName);

    if (index >= elements.size())
      return null;

    Element firstNode = elements.get(index);

    return firstNode.getTextContent();
  }

  public static String extractFirstValueToString(final Document doc,
      final String tagName) {
    return extractValueToString(doc, tagName, 0);
  }
}
