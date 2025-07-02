/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import com.google.common.collect.Lists;

public class SmsAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "sms-authenticator";
    private static final List<ProviderConfigProperty> configProperties = Lists.newArrayList();

    private String smsDevReceiver;

    static {
        configProperties.addAll(List.of(
                new ProviderConfigProperty("length", "Code length",
                        "The number of digits of the generated code.",
                        ProviderConfigProperty.STRING_TYPE, 6),
                new ProviderConfigProperty("ttl", "Time-to-live",
                        "The time to live in seconds for the code to be valid.",
                        ProviderConfigProperty.STRING_TYPE, "300"),
                new ProviderConfigProperty("mfa_session_ttl", "MFA Session Time-to-live",
                        "The time to live in days for the mfa session is valid on same device.",
                        ProviderConfigProperty.STRING_TYPE, ""),
                new ProviderConfigProperty("senderId", "SenderId",
                        "The sender ID is displayed as the message sender on the receiving device.",
                        ProviderConfigProperty.STRING_TYPE, "Keycloak"),
                new ProviderConfigProperty("apiClientReference", "SMS API Client REF",
                        "The client reference is configured to have the message service enabled.",
                        ProviderConfigProperty.PASSWORD, ""),
                new ProviderConfigProperty("apiUrl", "SMS API Endpoint",
                        "The API endpoint url is configured to have the message service enabled.",
                        ProviderConfigProperty.STRING_TYPE, ""),
                new ProviderConfigProperty("x-functions-key", "SMS API X-Function-Key",
                        "The API function header is configured to have the message service enabled.",
                        ProviderConfigProperty.PASSWORD,
                        ""),
                new ProviderConfigProperty("simulation", "Simulation mode",
                        "In simulation mode, the SMS won't be sent, but printed to the server logs",
                        ProviderConfigProperty.BOOLEAN_TYPE, true)));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "SMS Authentication";
    }

    @Override
    public String getHelpText() {
        return "Validates an OTP sent via SMS to the users mobile phone.";
    }

    @Override
    public String getReferenceCategory() {
        return OTPCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SmsAuthenticator(smsDevReceiver);
    }

    @Override
    public void init(Config.Scope config) {
        smsDevReceiver = config.get("smsDevReceiver");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
