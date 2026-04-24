package com.github.luischavez.videodownloader.app.mail;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.AppConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.github.luischavez.videodownloader.util.FileUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public final class MailSender {

    private static void sendMail(Context context, AppConfiguration appConfiguration, String subject, String body) {
        final String username = appConfiguration.getEmail();
        final String password = CryptoUtils.base64Decode(appConfiguration.getPassword());
        final String to = appConfiguration.getDistributionList().stream().collect(Collectors.joining(", "));

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject(subject);
            message.setContent(body, "text/html");

            Transport.send(message);
        } catch (Exception ex) {
            context.error(MailSender.class,"can't send mail to: " + to, ex);
        }
    }

    public static void send(Context context, String subject, String body) {
        try {
            AppConfiguration appConfiguration = context.getSystem().getManager(ConfigurationManager.class).get(AppConfiguration.class);

            if (appConfiguration == null) return;

            sendMail(context, appConfiguration, subject, body);
        } catch (Exception ex) {
            context.error(MailSender.class, "", ex);
        }
    }

    public static String buildFromTemplate(Context context, String template, Map<String, Object> params, String defaultIfNotFound) {
        String templateContent = FileUtils.fileAsString(template, context.buildPath(context.getWorkingDir(), "mails"));

        if (templateContent == null) templateContent = FileUtils.resourceAsString(MailSender.class, "/mails/" + template);

        if (templateContent == null) return defaultIfNotFound;

        Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();

            final String varName = entry.getKey();
            final Object varValue = entry.getValue();

            templateContent = templateContent.replace(String.format("{{%s}}", varName), varValue.toString());
        }

        return templateContent;
    }
}
