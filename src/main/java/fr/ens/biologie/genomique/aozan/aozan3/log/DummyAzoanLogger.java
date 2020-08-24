package fr.ens.biologie.genomique.aozan.aozan3.log;

import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;

/**
 * This class implements a dummy AozanLogger
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DummyAzoanLogger implements AozanLogger {

  @Override
  public void debug(String message) {
  }

  @Override
  public void info(String message) {
  }

  @Override
  public void warn(String message) {
  }

  @Override
  public void error(String message) {
  }

  @Override
  public void error(Throwable exception) {
  }

  @Override
  public void debug(RunId runId, String message) {
  }

  @Override
  public void info(RunId runId, String message) {
  }

  @Override
  public void warn(RunId runId, String message) {
  }

  @Override
  public void error(RunId runId, String message) {
  }

  @Override
  public void error(RunId runId, Throwable exception) {
  }

  @Override
  public void debug(RunData runData, String message) {
  }

  @Override
  public void info(RunData runData, String message) {
  }

  @Override
  public void warn(RunData runData, String message) {
  }

  @Override
  public void error(RunData runData, String message) {
  }

  @Override
  public void error(RunData runData, Throwable exception) {
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
  }

}
