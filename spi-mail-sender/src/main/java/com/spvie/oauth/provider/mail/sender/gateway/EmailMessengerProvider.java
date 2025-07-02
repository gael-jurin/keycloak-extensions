/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.sender.gateway;

import java.util.Objects;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spvie.oauth.provider.mail.sender.dto.EmailMessage;

public class EmailMessengerProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailMessengerProvider.class);

    public static void  transformAndSend(JSONObject body, String sender, String apiUrl, String receiver,
                                         String apiKey, String replyTo, String template, String subject)
            throws Exception {
        Objects.requireNonNull(body);
        Objects.requireNonNull(receiver);

        try {
            EmailMessage emailMessage = EmailMessage.Builder.newInstance()
                    .withSubject(subject)
                    .withFrom(sender)
                    .withTos(receiver)
                    .withReplyTo(replyTo)
                    .withTemplate(template)
                    .withData(body).build();

            MailNotificationClient notificationClient = new MailNotificationClient(apiUrl, apiKey);
            notificationClient.sendEmailMessage(emailMessage.getMsg());
        } catch (Exception mailException) {
            log.error(" Error while sending mail ",mailException);
            throw new Exception(mailException);
        }
    }

    public static void  transformAndSend(String body, String sender, String apiUrl, String receiver,
                                         String apiKey, String replyTo, String template, String subject)
            throws Exception {
        Objects.requireNonNull(body);
        Objects.requireNonNull(receiver);

        try {
            EmailMessage emailMessage = EmailMessage.Builder.newInstance()
                    .withSubject(subject)
                    .withFrom(sender)
                    .withTos(receiver)
                    .withReplyTo(replyTo)
                    .withTextBody(body).build();

            MailNotificationClient notificationClient = new MailNotificationClient(apiUrl, apiKey);
            notificationClient.sendEmailMessage(emailMessage.getMsg());
        } catch (Exception mailException) {
            log.error(" Error while sending mail ",mailException);
            throw new Exception(mailException);
        }
    }
}
