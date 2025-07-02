/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Objects;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spvie.oauth.provider.action.MobileNumberRequiredAction;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;

public class LegacyAuthenticator extends UsernamePasswordForm {
    private static final Logger log = LoggerFactory.getLogger(LegacyAuthenticator.class);
    private static final String PASSWORD_FELD = "password";
    private MobileNumberRequiredAction numberRequiredAction;

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);

        if (Objects.isNull(numberRequiredAction)) {
            numberRequiredAction = (MobileNumberRequiredAction)
                    context.getSession().getProvider(RequiredActionProvider.class, "mobile-number-ra");
        }

        if (Objects.isNull(user)) {
            return validateUserAndPassword(context, inputData);
        }

        if (user.getRequiredActionsStream().noneMatch(s -> s.equals(
                UserModel.RequiredAction.UPDATE_PASSWORD.name()))) {
            // If user doesn't have an UpdatePassword required action - Old password not applicable
            user.removeAttribute(PASSWORD_FELD);
        }

        String oldPassword = user.getFirstAttribute(PASSWORD_FELD);

        if (Objects.isNull(oldPassword)) {
            log.info("user {} credentials are migrated from CGRM database to Keycloak ...", user.getUsername());

            if (validateUser(context, inputData)) {
                numberRequiredAction.autoLoadMobileNumber(user, new StringBuilder());
            }
            return validateUserAndPassword(context, inputData);
        }

        if (validateUser(context, inputData) && oldPassword
                .equals(encryptPassword(inputData.getFirst(CredentialRepresentation.PASSWORD)))) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            user.removeAttribute(PASSWORD_FELD);
            return true;
        }

        return validateUserAndPassword(context, inputData);
    }

    @Override
    protected boolean isDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        String bruteForceError = getDisabledByBruteForceEventError(context, user);
        if (bruteForceError != null) {
            context.getEvent().user(user);
            context.getEvent().error(bruteForceError);
            Response challengeResponse = challenge(context, disabledByBruteForceError());
            context.forceChallenge(challengeResponse);
            return true;
        }
        return false;
    }

    @Override
    protected String disabledByBruteForceError() {
        return Messages.ACCOUNT_TEMPORARILY_DISABLED;
    }

    private String encryptPassword(String passwordText) {
        byte[] defaultBytes = passwordText.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(defaultBytes);
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0".concat(hashtext);
            }
            return new StringBuilder(hashtext).reverse().toString();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return passwordText;
    }
}
