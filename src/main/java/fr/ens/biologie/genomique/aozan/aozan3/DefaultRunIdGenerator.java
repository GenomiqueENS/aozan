package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Objects;

/**
 * This class define the default implementation of the RunIsGenerator interface.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DefaultRunIdGenerator implements RunIdGenerator {

  private static final String DEFAULT_EXPR = "${original.run.id}";

  private final String expr;

  @Override
  public RunId newRunId(RunId runId, Map<String, String> constants)
      throws Aozan3Exception {

    requireNonNull(runId);
    requireNonNull(constants);

    constants.put("run.id", runId.getId());
    constants.put("original.run.id", runId.getId());

    String newRunId = evaluateExpressions(this.expr, constants);

    return new RunId(newRunId, runId.getOriginalRunId());
  }

  //
  // Parsing methods
  //

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @return a string with expression evaluated
   * @throws Aozan3Exception if an error occurs while parsing the string or
   *           executing an expression
   */
  private String evaluateExpressions(final String s,
      final Map<String, String> constants) throws Aozan3Exception {

    if (s == null) {
      return null;
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
          if (constants.containsKey(trimmedExpr)) {
            result.append(constants.get(trimmedExpr));
          }

          i += expr.length() + 2;
          continue;
        }
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
  }

  private String subStr(final String s, final int beginIndex,
      final int charPoint) throws Aozan3Exception {

    final int endIndex = s.indexOf(charPoint, beginIndex);

    if (endIndex == -1) {
      throw new Aozan3Exception(
          "Unexpected end of expression in \"" + s + "\"");
    }

    return s.substring(beginIndex, endIndex);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  public DefaultRunIdGenerator() {
    this.expr = DEFAULT_EXPR;
  }

  /**
   * Constructor.
   * @param expr expression to use to create the new run id
   */
  public DefaultRunIdGenerator(String expr) {

    requireNonNull(expr);

    if (expr == null || expr.trim().isEmpty()) {
      throw new IllegalArgumentException("expr cannot be empty");
    }

    this.expr = expr;
  }

}
