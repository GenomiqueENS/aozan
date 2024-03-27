package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Splitter;

/**
 * This class allow to generate EmailMessage from templates.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class TemplateEmailMessage {

  public static final String DEFAULT_SUBJECT_PREFIX = "[Aozan] ";
  public static final String DEFAULT_MAIL_HEADER =
      "THIS IS AN AUTOMATED MESSAGE.\n";
  public static final String DEFAULT_MAIL_FOOTER = "\nThe Aozan team.\n";

  private String subjectPrefix = DEFAULT_SUBJECT_PREFIX;
  private String header = DEFAULT_MAIL_HEADER;
  private String footer = DEFAULT_MAIL_FOOTER;
  private String template = "";

  //
  // Setters
  //

  /**
   * Set the header of the message.
   * @param header the footer of the message
   */
  public void setHeader(String header) {

    requireNonNull(header);
    this.header = header.replace("\\n", "\n").replace("\\t", "\t");
  }

  /**
   * Set the footer of the message.
   * @param footer the footer of the message
   */
  public void setFooter(String footer) {

    requireNonNull(footer);
    this.footer = footer.replace("\\n", "\n").replace("\\t", "\t");
  }

  /**
   * Set the template of the message.
   * @param template the template of the message
   */
  public void setTemplate(String template) {

    requireNonNull(template);
    this.template = template;
  }

  /**
   * Set the template from a resource file.
   * @param resource path of the resource file
   * @throws IOException if an error occurs while reading the resource
   */
  public void setTemplateFromResource(String resource) throws IOException {

    requireNonNull(resource);
    setTemplate(readResource(resource));
  }

  /**
   * Set the template from a resource file.
   * @param templateFile path of the template file
   * @throws IOException if an error occurs while reading the tenplate file
   */
  public void setTemplateFromFile(Path templateFile) throws IOException {

    requireNonNull(templateFile);

    List<String> lines = Files.readAllLines(templateFile);

    setTemplate(readResource(String.join("\n", lines)));
  }

  //
  // Utility methods
  //

  /**
   * Read a text file resource.
   * @param resource path of the resource
   * @return a String with the content of the resource
   * @throws IOException if an error occurs while reading the resource
   */
  private static String readResource(String resource) throws IOException {

    StringBuilder sb = new StringBuilder();

    try (
        InputStream is =
            TemplateEmailMessage.class.getResourceAsStream(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

      String line;
      boolean first = false;
      while ((line = reader.readLine()) != null) {

        if (!first) {
          sb.append('\n');
          first = false;
        }
        sb.append(line);

      }

      return sb.toString();
    }
  }

  /**
   * Define a preprocessor for templates
   * @param s string to parse
   * @param variables variables for the preprocessor
   * @return the string preprocessed
   * @throws Aozan3Exception if an error occurs while preprocessing the string
   */
  private static String preprocessor(final String s,
      Map<String, String> variables) throws Aozan3Exception {

    requireNonNull(s);
    requireNonNull(variables);

    StringBuilder sb = new StringBuilder();
    Splitter lineSplitter = Splitter.on('\n');
    Splitter directiveSplitter =
        Splitter.on(' ').trimResults().omitEmptyStrings();

    boolean save = true;
    boolean inIf = false;
    int lineCount = 0;
    for (String line : lineSplitter.splitToList(s)) {

      lineCount++;
      if (!line.isEmpty() && line.charAt(0) == '#') {

        List<String> elements =
            directiveSplitter.splitToList(line.substring(1));

        if (!elements.isEmpty()) {

          String variable = elements.size() > 1
              ? elements.get(1).replace("$", "").replace("{", "").replace("}",
                  "")
              : null;

          switch (elements.get(0).toLowerCase()) {

          case "if-set":
            if (inIf || variable == null) {
              throw new Aozan3Exception(
                  "Email template error line " + lineCount + ": " + line);
            }

            inIf = true;
            if (variables.containsKey(variable)) {
              save = true;
            }
            continue;

          case "if-not-set":
            if (inIf || variable == null) {
              throw new Aozan3Exception(
                  "Email template error line " + lineCount + ": " + line);
            }

            inIf = true;
            if (variables.containsKey(variable)) {
              save = false;
            }
            continue;

          case "fi":
            if (!inIf) {
              throw new Aozan3Exception(
                  "Email template error line " + lineCount + ": " + line);
            }
            inIf = false;
            save = true;
            continue;

          default:
            break;

          }

        }

      }

      if (save) {
        if (lineCount != 1) {
          sb.append('\n');
        }
        sb.append(line);
      }
    }

    return sb.toString();

  }

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @param dict map with variables to replace
   * @return a string with expression evaluated
   * @throws Aozan3Exception
   */
  private static String evaluateExpressions(final String s,
      Map<String, String> variables) throws Aozan3Exception {

    requireNonNull(s);
    requireNonNull(variables);

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
          if (variables.containsKey(trimmedExpr)) {
            result.append(variables.get(trimmedExpr));
          }

          i += expr.length() + 2;
          continue;
        }
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
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

  //
  // Email generation methods
  //

  /**
   * Generate EmailMessage object.
   * @param subject subject of the message
   * @param variables variables for the template
   * @throws Aozan3Exception
   */
  public EmailMessage toEmailMessage(String subject,
      Map<String, String> variables) throws Aozan3Exception {

    return toEmailMessage(0L, subject, variables);
  }

  /**
   * Generate EmailMessage object.
   * @param id id of the email
   * @param subject subject of the message
   * @param variables variables for the template
   * @throws Aozan3Exception
   */
  public EmailMessage toEmailMessage(long id, String subject,
      Map<String, String> variables) throws Aozan3Exception {

    StringBuilder sb = new StringBuilder();

    if (!this.header.isBlank()) {
      sb.append(this.header);
      sb.append('\n');
    }

    sb.append(evaluateExpressions(
        preprocessor(this.template == null ? "" : this.template,
            variables == null ? Collections.emptyMap() : variables),
        variables));

    if (!this.footer.isBlank()) {
      sb.append('\n');
      sb.append(this.footer);
    }

    return new EmailMessage(id, this.subjectPrefix + subject, sb.toString());
  }

  /**
   * Generate error EmailMessage object.
   * @param t exception
   */
  public EmailMessage errorMessage(Throwable t) {

    String subject =
        this.subjectPrefix + "Error: " + t.getMessage().replace('\n', ' ');

    String errorMessage = errorMessage(this.header, this.footer, t);

    return new EmailMessage(0L, subject, errorMessage, true);

  }

  //
  // Other methods
  //

  /**
   * Create error message content.
   * @param t exception
   * @return a String with the error message content
   */
  private String errorMessage(String header, String footer, Throwable t) {

    requireNonNull(t);

    StringBuilder sb = new StringBuilder();
    if (header != null && !header.isBlank()) {
      sb.append(header);
    }

    sb.append("An exception has occured while executing Aozan: ");
    sb.append(t.getMessage());
    sb.append("\nStacktrace:\n");

    // StackTrace
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    sb.append(sw);

    if (footer != null && !footer.isBlank()) {
      sb.append(footer);
    }

    return sb.toString();
  }

  //
  // Static constructor
  //

  public static EmailMessage errorMessage(Configuration conf, Throwable t) {

    return new TemplateEmailMessage().errorMessage(t);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public TemplateEmailMessage() {
  }

  /**
   * Public constructor.
   * @param conf configuration
   */
  public TemplateEmailMessage(Configuration conf) {

    Objects.requireNonNull(conf);

    this.subjectPrefix =
        conf.get("mail.subject.prefix", DEFAULT_SUBJECT_PREFIX);
    this.header = conf.get("mail.header", DEFAULT_MAIL_HEADER);
    this.footer = conf.get("mail.footer", DEFAULT_MAIL_FOOTER);
  }

}
