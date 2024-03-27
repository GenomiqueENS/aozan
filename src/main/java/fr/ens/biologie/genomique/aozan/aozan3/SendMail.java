package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class is used to send emails
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SendMail {

  private final boolean sendMail;
  private final boolean printMail;
  private final Properties properties;
  private final String fromMail;
  private final List<String> toMail;
  private final List<String> errorToMail;

  private final Path lastErrorFile;

  private GenericLogger logger;

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

    if (email.isErrorMessage()) {

      if (this.lastErrorFile != null) {

        String text = email.getSubject() + '\n' + email.getContent();

        if (Files.isRegularFile(this.lastErrorFile)) {

          // Read last error message
          String lastText =
              readLastErrorMessage(this.lastErrorFile, this.logger);

          // Check if the error message has changed
          if (!text.equals(lastText)) {
            sendMail(email.getSubject(), email.getContent(), true);
            writeLastErrorMessage(this.lastErrorFile, text, this.logger);
          }

        } else {
          // Write new error message
          sendMail(email.getSubject(), email.getContent(), true);
          writeLastErrorMessage(this.lastErrorFile, text, this.logger);
        }

      } else {
        sendMail(email.getSubject(), email.getContent(), true);
      }
    } else {
      sendMail(email.getSubject(), email.getContent());
    }
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

      // Set recipients
      for (String email : error ? this.errorToMail : this.toMail) {
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
      }

      msg.setSubject(subject);
      msg.setSentDate(new Date());

      // Set message content
      msg.setText(textContent);

      // Send the message
      // Transport.send(msg);
      Transport tr = session.getTransport("smtp");
      tr.connect(this.properties.getProperty("mail.smtp.host"),
          this.properties.getProperty("mail.smtp.login"),
          this.properties.getProperty("mail.smtp.password"));
      msg.saveChanges();
      tr.sendMessage(msg, msg.getAllRecipients());
      tr.close();

    } catch (MessagingException mex) {
      this.logger.warn("Error while sending mail: " + mex.getMessage());
    }

    if (!error
        && this.lastErrorFile != null
        && Files.isRegularFile(this.lastErrorFile)) {

      try {
        Files.delete(this.lastErrorFile);
      } catch (IOException e) {
        this.logger.warn(
            "Error while removing last error file: " + this.lastErrorFile);
      }

    }

  }

  //
  // Other methods
  //

  /**
   * Read last error message content.
   * @param lastErrorFile last error message file
   * @param logger logger
   * @return a String with the error email content
   */
  private static String readLastErrorMessage(Path lastErrorFile,
      GenericLogger logger) {

    try {
      return new String(Files.readAllBytes(lastErrorFile),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.warn("Error while reading last error file: " + lastErrorFile);
      return null;
    }
  }

  /**
   * Write last error message content.
   * @param lastErrorFile last error message file
   * @param msg message to write
   * @param logger logger
   */
  private static void writeLastErrorMessage(Path lastErrorFile, String msg,
      GenericLogger logger) {

    try {
      Files.write(lastErrorFile, msg.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      logger.warn("Error while writing last error file: " + lastErrorFile);
    }
  }

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

    // Set default SMTP port if not set
    if (!result.containsKey("mail.smtp.port")) {
      result.setProperty("mail.smtp.port", "25");
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

  /**
   * Convert an email string to an email list.
   * @param emails a list of email in a string separated by commas
   * @return a list of email
   */
  private List<String> toEmailList(String emails) {

    if (emails == null) {
      return emptyList();
    }

    return Splitter.on(',').trimResults().omitEmptyStrings()
        .splitToList(emails);
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
  public SendMail(Configuration conf, GenericLogger logger)
      throws Aozan3Exception {

    requireNonNull(conf);

    this.logger = logger != null ? logger : new DummyLogger();

    this.printMail = conf.getBoolean("print.mail", false);
    this.sendMail = conf.getBoolean("send.mail", false);

    this.fromMail = conf.get("mail.from", "");
    this.toMail = toEmailList(conf.get("mail.to", ""));
    this.errorToMail =
        toEmailList(conf.get("mail.error.to", conf.get("mail.to", "")));

    this.properties = createJavaMailSMTPProperties(conf);

    this.lastErrorFile = conf.containsKey("mail.last.error.file")
        ? new File(conf.get("mail.last.error.file")).toPath() : null;

    // Check Email configuration
    checkConfiguration();
  }

}
