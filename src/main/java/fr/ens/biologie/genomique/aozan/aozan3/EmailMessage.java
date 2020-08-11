package fr.ens.biologie.genomique.aozan.aozan3;

import java.util.Objects;

/**
 * This class define define an email message.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class EmailMessage {

  private final boolean noMessage;
  private final long id;
  private final String subject;
  private final String content;

  /**
   * Get the id of the message.
   * @return the id of the message
   */
  public long getId() {
    return this.id;
  }

  /**
   * Get the subject of the message.
   * @return the subject of the message
   */
  public String getSubject() {
    return this.subject;
  }

  /**
   * Get the content of the message.
   * @return the content of the message
   */
  public String getContent() {
    return this.content;
  }

  /**
   * Test if the message must be sent.
   * @return true if the message must not be sent
   */
  public boolean isNoMessage() {
    return this.noMessage;
  }

  //
  // Static constructor
  //

  /**
   * Create a email without message that will be sent.
   * @return a email without message that will be sent
   */
  public static EmailMessage noMessage() {

    return new EmailMessage();
  }

  //
  // Constructor
  //

  /**
   * Private constructor for a email without message.
   */
  private EmailMessage() {

    this.noMessage = true;
    this.id = 0L;
    this.subject = "";
    this.content = "";
  }

  /**
   * Constructor.
   * @param subject subject of the message
   * @param content content of the message
   */
  public EmailMessage(String subject, String content) {

    this(0L, subject, content);
  }

  /**
   * Constructor.
   * @param id id of the email
   * @param subject subject of the message
   * @param content content of the message
   */
  public EmailMessage(long id, String subject, String content) {

    Objects.requireNonNull(subject);
    Objects.requireNonNull(content);

    this.noMessage = false;
    this.id = id;
    this.subject = subject;
    this.content = content;
  }

}
