/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.challenge.BasicAuthAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

public class LegacyAuthenticatorFactory extends BasicAuthAuthenticatorFactory {
    public static final String PROVIDER_ID = "legacy-authenticator";
    public static final LegacyAuthenticator SINGLETON = new LegacyAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Legacy CGRMWEB Authentication";
    }

    @Override
    public String getHelpText() {
        return "Validates the password used in CGRMWEB.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }
}
