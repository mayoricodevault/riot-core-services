package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class allows to send emails
 */
public class EmailSender extends Thread {
    protected static final Logger logger = Logger.getLogger(EmailSender.class);

    public static class SmtpParameters {
        String host;
        int port = 25;
        String userName;
        String password;
        boolean ssl = false;
        boolean tls = false;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isSsl() {
            return ssl;
        }

        public void setSsl(boolean ssl) {
            this.ssl = ssl;
        }

        public boolean isTls() {
            return tls;
        }

        public void setTls(boolean tls) {
            this.tls = tls;
        }
    }

    public static class Attachment {
        String fileName;
        String mimeType;
        File file;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }

    public static class EmailMessageParameters {
        String contentType;
        String from;
        String toEmail;
        String toName;
        String subject;
        String msg;
        Collection<Attachment> attachments = new ArrayList<>();
        Collection<InternetAddress> to = null;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getToEmail() {
            return toEmail;
        }

        public void setToEmail(String toEmail) {
            this.toEmail = toEmail;
        }

        public String getToName() {
            return toName;
        }

        public void setToName(String toName) {
            this.toName = toName;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public Collection<InternetAddress> getTo() {
            return to;
        }

        public void setTo(Collection<InternetAddress> to) {
            this.to = to;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setTo(String... emails) {
            List<InternetAddress> addresses = new ArrayList<>();
            if (emails != null) {
                for (String email : emails) {
                    try {
                        if (email != null) {
                            InternetAddress e1 = new InternetAddress(email.trim());
                            e1.validate();
                            addresses.add(e1);
                        }
                    } catch (AddressException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            this.to = addresses;
        }

        public Collection<Attachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(Collection<Attachment> attachments) {
            this.attachments = attachments;
        }
    }

    public static SmtpParameters getSmtpGmailParameters() {
        SmtpParameters parameters = new SmtpParameters();
        parameters.setHost("smtp.gmail.com");
        parameters.setPort(465);
        parameters.setSsl(true);
        parameters.setTls(true);
        return parameters;
    }

    public static SmtpParameters getTierconnectParameters() {
        SmtpParameters parameters = new SmtpParameters();
        parameters.setHost("tcexchange2010.tierconnect.com");
        return parameters;
    }

    SmtpParameters smtpParameters;

    public EmailSender(SmtpParameters smtpParameters) {
        this.smtpParameters = smtpParameters;
    }

    public void send(EmailMessageParameters parameters) throws EmailException, UserException {
        try {
            SimpleEmail email = new SimpleEmail();
            email.setAuthentication(smtpParameters.getUserName(), smtpParameters.getPassword());
            email.setHostName(smtpParameters.getHost());
            email.setSmtpPort(smtpParameters.getPort());
            email.setStartTLSEnabled(smtpParameters.isTls());
            email.setSSLOnConnect(smtpParameters.isSsl());
            email.setFrom(parameters.getFrom());
            if (parameters.getAttachments() != null && parameters.getAttachments().size() > 0) {
                BodyPart messageBodyPart = new MimeBodyPart();
                try {
                    if (StringUtils.isBlank(parameters.getContentType())) {
                        messageBodyPart.setText(parameters.getMsg());
                    } else {
                        messageBodyPart.setContent(parameters.getMsg(), parameters.getContentType());
                    }
                } catch (MessagingException e) {
                    logger.error(e.getMessage());
                }
                MimeMultipart multipart = new MimeMultipart();
                try {
                    multipart.addBodyPart(messageBodyPart);
                } catch (MessagingException e) {
                    logger.error(e.getMessage());
                }
                for (Attachment attachment : parameters.getAttachments()) {
                    BodyPart messageBodyPart2 = new MimeBodyPart();
                    DataSource source = new FileDataSource(attachment.getFile());
                    try {
                        messageBodyPart2.setDataHandler(new DataHandler(source));
                        messageBodyPart2.setFileName(attachment.getFileName());
                        multipart.addBodyPart(messageBodyPart2);
                    } catch (MessagingException e) {
                        logger.error(e.getMessage());
                    }
                }
                email.setContent(multipart, "multipart/mixed");

            } else {
                if (StringUtils.isBlank(parameters.getContentType())) {
                    email.setMsg(parameters.getMsg());
                } else {
                    email.setContent(parameters.getMsg(), parameters.getContentType());
                }
            }
            if (parameters.getTo() != null) {
                email.setTo(parameters.getTo());
            } else {
                email.addTo(parameters.getToEmail(), parameters.getToName());
            }
            email.setSubject(parameters.getSubject());

            logger.debug( "sending email to " + parameters.getToName() + " using " + smtpParameters.getHost() + " with user" + smtpParameters.getUserName() );
            email.send();
        } catch (EmailException e) {
            if (e.getCause() instanceof AuthenticationFailedException) {
                logger.error("Error when trying to send a email. Authentication error was detected for username [" + smtpParameters.getUserName() + "].");
            } else {
                logger.error("Error when trying to send a email notification.", e);
            }
            throw e;
        }
    }

    public static void main(String[] args) throws  Exception{
        EmailSender.SmtpParameters mailParameters = EmailSender.getTierconnectParameters();
        mailParameters.setUserName("riottest@tierconnect.com");
        EmailSender e = new EmailSender(mailParameters);
        EmailSender.EmailMessageParameters messageParameters = new EmailSender.EmailMessageParameters();
        messageParameters.setFrom("riottest@tierconnect.com");
        messageParameters.setTo("aldo_gg@yahoo.com", "aldo_alf@hotmail.com");
        messageParameters.setToName("Aldo Gutierrez");
        messageParameters.setSubject("test3");
        messageParameters.setMsg("acaba3");
        e.send(messageParameters);
    }
}
