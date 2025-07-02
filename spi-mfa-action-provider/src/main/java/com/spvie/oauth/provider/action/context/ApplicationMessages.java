/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.action.context;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationMessages {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationMessages.class);
    private Properties messages;

    public ApplicationMessages(RequiredActionContext context, Theme.Type themeType) {
        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();
        Locale locale = session.getContext().resolveLocale(user);
        try {
            messages = session.theme().getTheme(themeType).getMessages(locale);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public String getMessage(String key) {
        return messages.getProperty(key, key);
    }
}
