/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator.gateway;

import java.util.Map;
import java.util.UUID;

import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OTPNotificationService implements NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final KeycloakSession session;
    private final String senderId;
    private final String apiUrl;
    private final String apiKey;
    private final String apiClientReference;
    private final NotificationClient messagingClient;

    public OTPNotificationService(Map<String, String> config,
                                  KeycloakSession session) {
        this.session = session;
        this.senderId = config.get("senderId");
        this.apiClientReference = String.valueOf(UUID.randomUUID());
        this.apiUrl = config.get("apiUrl");
        this.apiKey = config.get("x-functions-key");
        this.messagingClient = new NotificationClient(apiClientReference, apiUrl, apiKey);
    }

    @Override
    public void send(String phoneNumber, String message) {
        try {
            messagingClient.sendTextMessage(message, senderId, new String[] { phoneNumber});
        } catch (Exception e) {
            // TODO: sendOtpMail(message) as alternative if this backup canal is approved by the RSSI;
            LOG.error(e.getMessage());
        }
    }

}
