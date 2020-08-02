package fr.ens.biologie.genomique.aozan.aozan3;

/**
 * This class define an Aozan exception
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Aozan3Exception extends Exception {

  private static final long serialVersionUID = -3846631215651424819L;

  /**
   * Create a new Aozan3Exception.
   */
  public Aozan3Exception() {

    super();
  }

  /**
   * Create a new Aozan3Exception with a message.
   * @param message the message
   */
  public Aozan3Exception(final String message) {

    super(message);
  }

  /**
   * Create a new Aozan3Exception with a message and a cause.
   * @param message the message
   * @param cause the cause
   */
  public Aozan3Exception(String message, Throwable cause) {

    super(message, cause);
  }

  /**
   * Create a new Aozan3Exception with a cause.
   * @param cause the cause
   */
  public Aozan3Exception(Throwable cause) {

    super(cause);
  }

  /**
   * Create a new Aozan3Exception with a message.
   * @param runId run id
   * @param message the message
   */
  public Aozan3Exception(final RunId runId, final String message) {

    super(message);
  }

  /**
   * Create a new Aozan3Exception with a message and a cause.
   * @param runId run id
   * @param message the message
   * @param cause the cause
   */
  public Aozan3Exception(final RunId runId, String message, Throwable cause) {

    super(message, cause);
  }

  /**
   * Create a new Aozan3Exception with a cause.
   * @param runId run id
   * @param cause the cause
   */
  public Aozan3Exception(final RunId runId, Throwable cause) {

    super(cause);
  }

}
