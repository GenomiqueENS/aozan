package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;

/**
 * This class contains Utility methods to parse XML documents.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ParserUtils {

  /**
   * Check if the child tags of a tag is in a list of allowed tag names.
   * @param element the tag element
   * @param tagNames the allowed tag names
   * @throws Aozan3Exception if a child tag of the tag in not in the allowed tag
   *           list
   */
  static void checkAllowedChildTags(final Element element, String... tagNames)
      throws Aozan3Exception {

    requireNonNull(element);
    requireNonNull(tagNames);

    final List<String> tagList = Arrays.asList(tagNames);

    final NodeList nl = element.getChildNodes();

    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);

      if (n.getNodeType() == Node.ELEMENT_NODE) {

        final String childTagName = n.getNodeName();

        if (!tagList.contains(childTagName)) {
          throw new Aozan3Exception("the \""
              + element.getNodeName() + "\" tag contains an unknown tag: "
              + childTagName + ".");
        }
      }
    }
  }

  /**
   * Check if the attribute of a tag is in a list of allowed attribute names.
   * @param element the tag element
   * @param attributeNames the allowed attribute names
   * @throws Aozan3Exception if an attribute of the tag in not in the allowed
   *           attribute list
   */
  static void checkAllowedAttributes(final Element element,
      String... attributeNames) throws Aozan3Exception {

    requireNonNull(element);
    requireNonNull(attributeNames);

    final List<String> attributeList = Arrays.asList(attributeNames);

    final NamedNodeMap nnm = element.getAttributes();

    for (int i = 0; i < nnm.getLength(); i++) {

      final Node n = nnm.item(i);

      if (n.getNodeType() == Node.ATTRIBUTE_NODE) {

        final String attributeName = n.getNodeName();

        if (!attributeList.contains(attributeName)) {
          throw new Aozan3Exception("the \""
              + element.getNodeName() + "\" tag contains an unknown attribute: "
              + attributeName + ".");
        }
      }
    }
  }

  /**
   * Get the value of a tag
   * @param tag name of the tag
   * @param element root element
   * @return the value of the tag
   */
  static String getTagValue(final String tag, final Element element) {

    requireNonNull(tag);
    requireNonNull(element);

    final NodeList nl = element.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName())) {
        return n.getTextContent();
      }
    }

    return null;
  }

  /**
   * Return an empty string if string parameter is null.
   * @param s string to evaluate
   * @return the orignal string or an empty string if the string parameter is
   *         null
   */
  static String nullToEmpty(String s) {

    return Objects.toString(s, "");
  }

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @param allowExec allow execution of code
   * @return a string with expression evaluated
   * @throws Aozan3Exception if an error occurs while parsing the string or
   *           executing an expression
   */
  static String evaluateExpressions(final String s, Configuration conf)
      throws Aozan3Exception {

    if (s == null) {
      return null;
    }

    // Nothing to do if conf is null or empty
    if (conf == null || conf.isEmpty()) {
      return s;
    }

    final StringBuilder result = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final int c0 = s.codePointAt(i);

      // Variable substitution
      if (c0 == '$' && i + 1 < len) {

        final int c1 = s.codePointAt(i + 1);
        if (c1 == '{') {

          final String expr = subStr(s, i + 2, '}');

          final String trimmedExpr = expr.trim();
          if (conf.containsKey(trimmedExpr)) {
            result.append(conf.get(trimmedExpr));
          }

          i += expr.length() + 2;
          continue;
        }
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
  }

  //
  // Utility parsing methods
  //

  /**
   * Get the value of an XML tag attribute and evaluate it if necessary.
   * @param element the XML tag element
   * @param name the name of the attribute
   * @return the value of the attribute
   */
  static String getAttribute(Element element, final String name) {

    return element.getAttribute(name);
  }

  private static String subStr(final String s, final int beginIndex,
      final int charPoint) throws Aozan3Exception {

    final int endIndex = s.indexOf(charPoint, beginIndex);

    if (endIndex == -1) {
      throw new Aozan3Exception(
          "Unexpected end of expression in \"" + s + "\"");
    }

    return s.substring(beginIndex, endIndex);
  }

  /**
   * Return an unknown source string if the argument is null.
   * @param s string argument
   * @return the original string if null or an unknown message string
   */
  public static String nullToUnknownSource(String s) {

    if (s == null) {
      return "(Unknown source)";
    }

    return s;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private ParserUtils() {
  }

}
