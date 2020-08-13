package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This class is used to send emails
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SendMail {

  private static final String DEFAULT_SUBJECT_PREFIX = "[Aozan] ";
  private static final String DEFAULT_MAIL_HEADER =
      "THIS IS AN AUTOMATED MESSAGE.\n\n";
  private static final String DEFAULT_MAIL_FOOTER = "\n\nThe Aozan team.\n";

  private final boolean sendMail;
  private final boolean printMail;
  private final Properties properties;
  private final String fromMail;
  private final String toMail;
  private final String errorToMail;

  private final String subjectPrefix;
  private final String header;
  private final String footer;

  private AozanLogger logger;

  /**
   * Send an email with Aozan header and footer.
   * @param email the email message
   */
  public void sendMail(EmailMessage email) {

    requireNonNull(email);

    // Nothing to do if the email is empty
    if (email.isNoMessage()) {
      return;
    }

    sendMail(this.subjectPrefix + email.getSubject(),
        this.header + email.getContent() + this.footer);
  }

  /**
   * Send an email with the content of an exception
   * @param t the exception
   */
  public void sendMail(Throwable t) {

    requireNonNull(t);

    String subject =
        this.subjectPrefix + "Error: " + t.getMessage().replace('\n', ' ');

    StringBuilder sb = new StringBuilder();
    sb.append(this.header);

    sb.append("An exception has occured while executing Aozan: ");
    sb.append(t.getMessage());
    sb.append("\nStacktrace:\n");

    // StackTrace
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    sb.append(sw);

    sb.append(this.footer);

    sendMail(subject, sb.toString(), true);
  }

  /**
   * Send a message.
   * @param subject subject of the message
   * @param textContent text of the message
   */
  public void sendMail(String subject, String textContent) {

    sendMail(subject, textContent, false);
  }

  /**
   * Send a message.
   * @param subject subject of the message
   * @param textContent text of the message
   * @param error the message is an error
   */
  public void sendMail(String subject, String textContent, boolean error) {

    // TODO Handle file attachement

    requireNonNull(subject);
    requireNonNull(textContent);

    if (this.printMail) {
      printEmail(subject, textContent, error);
    }

    if (!this.sendMail) {
      return;
    }

    final Session session = Session.getInstance(this.properties);

    try {
      // Instantiate a message
      Message msg = new MimeMessage(session);

      // Set message attributes
      msg.setFrom(new InternetAddress(this.fromMail));
      InternetAddress[] address =
          {new InternetAddress(error ? this.errorToMail : this.toMail)};
      msg.setRecipients(Message.RecipientType.TO, address);
      msg.setSubject(subject);
      msg.setSentDate(new Date());

      // Set message content
      msg.setText(textContent);

      // Send the message
      Transport.send(msg);

    } catch (MessagingException mex) {
      this.logger.warn("Error while sending mail: " + mex.getMessage());
    }

  }

  //
  // Other methods
  //

  /**
   * Create a property object for javamail smtp configuration from the settings.
   * @return a property object
   */
  private static Properties createJavaMailSMTPProperties(Configuration conf) {

    final Properties result = new Properties();

    for (Map.Entry<String, String> e : conf.toMap().entrySet()) {

      final String key = e.getKey();

      final String prefix = "mail.smtp.";

      if (key != null && key.startsWith(prefix)) {
        result.setProperty(key, e.getValue());
      }

    }

    return result;
  }

  /**
   * Check email configuration.
   * @throws Aozan3Exception if the configuration is invalid
   */
  private void checkConfiguration() throws Aozan3Exception {

    if (!this.sendMail) {
      return;
    }

    if (!this.properties.containsKey("mail.smtp.host")) {
      throw new Aozan3Exception("No SMTP server set");
    }

    if (this.fromMail == null) {
      throw new Aozan3Exception("No \"From\" email set");
    }

    if (this.toMail == null) {
      throw new Aozan3Exception("No \"To\" email set");
    }

    if (this.errorToMail == null) {
      throw new Aozan3Exception("No \"To\" email set for errors");
    }

  }

  private void printEmail(String subject, String textContent, boolean error) {

    StringBuilder sb = new StringBuilder();

    sb.append("From: ");
    sb.append("".equals(this.fromMail) ? "(Not set)" : this.fromMail);
    sb.append("\nTo: ");

    if (error) {
      sb.append("".equals(this.errorToMail) ? "(Not set)" : this.errorToMail);
    } else {
      sb.append("".equals(this.toMail) ? "(Not set)" : this.toMail);
    }
    sb.append("\nSubject: ");
    sb.append(subject);
    sb.append("\n\n");
    sb.append(textContent);

    if (error) {
      System.err.println(sb);
    } else {
      System.out.println(sb);

    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf the configuration of the provider
   * @param logger the logger to use
   * @throws Aozan3Exception if an error occurs while initialize the provider
   */
  public SendMail(Configuration conf, AozanLogger logger)
      throws Aozan3Exception {

    requireNonNull(conf);

    this.logger = logger != null ? logger : new DummyAzoanLogger();

    this.printMail = conf.getBoolean("print.mail", false);
    this.sendMail = conf.getBoolean("send.mail", false);

    this.fromMail = conf.get("mail.from", "");
    this.toMail = conf.get("mail.to", "");
    this.errorToMail = conf.get("mail.error.to", this.toMail);

    this.subjectPrefix =
        conf.get("mail.subject.prefix", DEFAULT_SUBJECT_PREFIX);
    this.header = conf.get("mail.header", DEFAULT_MAIL_HEADER);
    this.footer = conf.get("mail.footer", DEFAULT_MAIL_FOOTER);

    this.properties = createJavaMailSMTPProperties(conf);

    // Check Email configuration
    checkConfiguration();
  }

}
