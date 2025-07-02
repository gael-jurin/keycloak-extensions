/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.sender.gateway;

import java.util.Map;

import org.json.JSONObject;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class EmailFacadeProvider implements EmailSenderProvider {
    private static final Logger log = LoggerFactory.getLogger(EmailFacadeProvider.class);

    private final KeycloakSession session;
    private String apiKey;
    private String senderId;
    private String template;
    private String apiClientReference;
    private String apiUrl;
    private String receiver;

    public EmailFacadeProvider(KeycloakSession session, String emailDevReceiver) {
        this.session = session;
        receiver = emailDevReceiver;
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody,
                     String htmlBody) throws EmailException {

        log.info("Attempting to send email  to {}", user.getEmail());
        try {
            senderConfig configs = getConfigs(config);

            template = textBody;

            if (Strings.isNullOrEmpty(template)) {
                EmailMessengerProvider.transformAndSend(htmlBody, configs.from(), apiUrl,
                        Strings.isNullOrEmpty(receiver) ? user.getEmail() : receiver,
                        apiKey, configs.reply(),
                        template, subject);
            } else {
                JSONObject textObj = new JSONObject(htmlBody);
                EmailMessengerProvider.transformAndSend(textObj, configs.from(), apiUrl,
                        Strings.isNullOrEmpty(receiver) ? user.getEmail() : receiver,
                        apiKey, configs.reply(),
                        template, subject);
            }

            log.info("email sent to {} successfully : {} ", user.getEmail(), textBody);
        } catch (Exception e) {
            log.error("Failed to send email to {}", user.getEmail());
            throw new EmailException(e.getMessage());
        }
    }

    @Override
    public void send(Map<String, String> map, String s, String s1, String s2, String s3) throws EmailException {
        log.info("email sent " + " successfully");
    }

    @Override
    public void close() {
        log.info("Close factory method");
    }

    private senderConfig getConfigs(Map<String, String> config) {
        this.senderId = config.get("senderId");
        this.apiClientReference = config.get("apiClientReference");
        this.apiUrl = config.get("host");
        this.apiKey = config.get("password");
        String from = config.get("from");
        String reply = config.get("replyTo");
        senderConfig configs = new senderConfig(from, reply);
        return configs;
    }

    private record senderConfig(String from, String reply) {
    }
}
