/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.template;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SendgridEmailTemplateProviderFactory implements EmailTemplateProviderFactory {

    private String pwdResetTempId;
    private String emailVerifTempId;
    private String exeActionsTempId;

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        return new SendgridEmailTemplateProvider(session, pwdResetTempId,
                emailVerifTempId, exeActionsTempId);
    }

    @Override
    public void init(Config.Scope config) {
        pwdResetTempId = config.get("pwdResetTempId");
        emailVerifTempId = config.get("emailVerifTempId");
        exeActionsTempId = config.get("exeActionsTempId");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

}
