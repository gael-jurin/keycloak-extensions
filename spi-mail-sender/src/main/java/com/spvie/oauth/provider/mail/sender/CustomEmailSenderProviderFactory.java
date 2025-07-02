/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.sender;

import org.keycloak.Config;
import org.keycloak.email.DefaultEmailSenderProviderFactory;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spvie.oauth.provider.mail.sender.gateway.EmailFacadeProvider;

public class CustomEmailSenderProviderFactory extends DefaultEmailSenderProviderFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CustomEmailSenderProviderFactory.class);
    private String emailDevReceiver;

    public CustomEmailSenderProviderFactory() {
        LOG.info("Init custom Email sender factory");
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new EmailFacadeProvider(session, emailDevReceiver);
    }

    @Override
    public void init(Config.Scope config) {
        LOG.info("Init config factory");
        emailDevReceiver = config.get("emailDevReceiver");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        LOG.info("Email Factory closed");
    }

    @Override
    public String getId() {
        return "default";
    }
}
