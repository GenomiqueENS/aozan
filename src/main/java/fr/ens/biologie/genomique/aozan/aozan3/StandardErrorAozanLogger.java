package fr.ens.biologie.genomique.aozan.aozan3;

import java.util.Date;

/**
 * This class define implements a logger on the standard error.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StandardErrorAozanLogger implements AozanLogger {

  @Override
  public void debug(String message) {
    System.err.println(new Date() + ", DEBUG: " + message);
  }

  @Override
  public void info(String message) {
    System.err.println(new Date() + ", INFO: " + message);
  }

  @Override
  public void warn(String message) {
    System.err.println(new Date() + ", WARN: " + message);
  }

  @Override
  public void error(String message) {
    System.err.println(new Date() + ", ERROR: " + message);
  }

  @Override
  public void error(Throwable exception) {
    System.err.println(new Date() + ", ERROR: " + exception.getMessage());
  }

  @Override
  public void debug(RunId runId, String message) {
    System.err
        .println(new Date() + ", [Run " + runId + "] , DEBUG: " + message);
  }

  @Override
  public void info(RunId runId, String message) {
    System.err.println(new Date() + ", [Run " + runId + "] , INFO: " + message);

  }

  @Override
  public void warn(RunId runId, String message) {
    System.err.println(new Date() + ", [Run " + runId + "] , WARN: " + message);

  }

  @Override
  public void error(RunId runId, String message) {
    System.err
        .println(new Date() + ", [Run " + runId + "] , ERROR: " + message);

  }

  @Override
  public void error(RunId runId, Throwable exception) {
    System.err.println(new Date()
        + ", [Run " + runId + "] , ERROR: " + exception.getMessage());

  }

}
