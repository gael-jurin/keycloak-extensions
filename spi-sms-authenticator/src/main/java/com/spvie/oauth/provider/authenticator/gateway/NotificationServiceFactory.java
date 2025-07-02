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

import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceFactory.class);
    public NotificationServiceFactory() {
    }

    public static NotificationService get(Map<String, String> config, KeycloakSession session) {
        if (Boolean.parseBoolean(config.getOrDefault("simulation", "false"))) {
            return (phoneNumber, message) ->
                    LOG.warn(String.format("***** SIMULATION MODE ***** Would send SMS to %s with text: %s",
                            phoneNumber, message));
        } else {
            return new OTPNotificationService(config, session);
        }
    }
}
