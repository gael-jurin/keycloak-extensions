/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.action;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MobileNumberRequiredActionFactory implements RequiredActionFactory {
    private static final String PROVIDER_ID = "mobile-number-ra";

    private String serviceUrl;
    private String serviceKey;

    @Override
    public RequiredActionProvider create(KeycloakSession keycloakSession) {
        return new MobileNumberRequiredAction(serviceUrl, serviceKey);
    }

    @Override
    public String getDisplayText() {
        return "Update mobile phone number";
    }

    @Override
    public void init(Config.Scope config) {
        serviceUrl = config.get("coreApiUrl");
        serviceKey = config.get("coreApiKey");
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
