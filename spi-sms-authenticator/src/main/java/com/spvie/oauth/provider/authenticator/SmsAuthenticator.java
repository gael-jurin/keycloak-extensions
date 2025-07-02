/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.spvie.oauth.provider.authenticator.dto.MfaSession;
import com.spvie.oauth.provider.authenticator.gateway.NotificationServiceFactory;

public class SmsAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(SmsAuthenticator.class);
    private static final String TPL_CODE = "login-sms.ftl";
    private static final String MOBILE_NUMBER_ATTRIBUTE = "mobile_number";
    private static final String PREVIOUS_MOBILE_ATTRIBUTE = "previous_mobile_number";
    private static final String MFA_SESSION_ATTRIBUTE = "mfa_session";
    private static final String CUSTOM_PARAM = "customParam";
    private static final String REALM = "realm";
    private static final String REPLAY = "replay";

    private String smsDevReceiver;

    public SmsAuthenticator(String smsDevReceiver) {
        this.smsDevReceiver = smsDevReceiver;
    }

    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        KeycloakSession session = context.getSession();
        UserModel user = context.getUser();

        int length = Integer.parseInt(config.getConfig().get("length"));
        int ttl = Integer.parseInt(config.getConfig().get("ttl"));

        String userDeviceId = getDeviceIdFromCookie(context);
        String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);

        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String smsText = String.format(smsAuthText, code, Math.floorDiv(ttl, 60));
            LOG.debug(smsText);

            if (Boolean.FALSE.equals(isMFARequired(context.getUser(), userDeviceId))) {
                LOG.info("Saisie du second facteur MFA non requis, session OK.");
                context.success();
            } else {
                LOG.info("Saisie du second facteur MFA requis");
                processMfaCheck(context, user, ttl, code, config);
            }
        } catch (Exception e) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("smsAuthSmsNotSent", e.getMessage())
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    private void processMfaCheck(AuthenticationFlowContext context, UserModel user, int ttl, String code,
                                 AuthenticatorConfigModel config) {
        if (context.getAuthenticationSession().getAuthNote("code") == null) {
            // Block refresh page sending SMS
            sendSMS(context, user, ttl, code);
        }

        context.challenge(context.form().setAttribute(CUSTOM_PARAM, getPhoneNumberSuffix(user))
                .setAttribute(REALM,
                        context.getRealm()).createForm(TPL_CODE));
        // Ecrit le code du sms dans un cookie si en mode simulation
        if (Boolean.parseBoolean(config.getConfig().getOrDefault("simulation", "false"))) {
            HttpResponse originalResponse = ResteasyProviderFactory.getInstance()
                    .getContextData(HttpResponse.class);
            originalResponse.addNewCookie(
                    new NewCookie("code sms", code, "/realms/" + context.getRealm().getName(),
                            null, null, ttl,
                            true, true));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("code");
        String noCode = context.getHttpRequest().getDecodedFormParameters().getFirst("no_code");
        String noPhone = context.getHttpRequest().getDecodedFormParameters().getFirst("no_phone");
        int mfaTtl = (int) getMfaConfigTtl(context);

        if ("update_phone".equals(noPhone) && Strings.isNullOrEmpty(enteredCode)) {
            context.getUser().setSingleAttribute(PREVIOUS_MOBILE_ATTRIBUTE,
                    context.getUser().getFirstAttribute(MOBILE_NUMBER_ATTRIBUTE));
            context.getUser().removeAttribute(MOBILE_NUMBER_ATTRIBUTE);
            setRequiredActions(context.getSession(), context.getRealm(), context.getUser());
            context.form().createForm("update-mobile-number.ftl");
            context.success();
            return;
        }

        if (manageCodeNotReceived(context, noCode, enteredCode) == null) {
            return;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String code = authSession.getAuthNote("code");
        String ttl = authSession.getAuthNote("ttl");

        if (code == null || ttl == null) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        boolean isValid = enteredCode.equals(code);
        if (isValid) {
            if (Long.parseLong(ttl) < System.currentTimeMillis()) {
                // expired
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        context.form().setAttribute(CUSTOM_PARAM, getPhoneNumberSuffix(context.getUser()))
                                .setAttribute(REALM, context.getRealm())
                                .setAttribute(REPLAY, context.getAuthenticationSession().getAuthNote(REPLAY))
                                .setError("smsAuthCodeExpired").createForm(TPL_CODE));
            } else {
                // valid
                // Enregistrement du ttl de session et du deviceId
                storeMFASession(context, mfaTtl);
                context.success();
            }
        } else {
            // invalid
            AuthenticationExecutionModel execution = context.getExecution();
            if (execution.isRequired()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setAttribute(CUSTOM_PARAM, getPhoneNumberSuffix(context.getUser()))
                                .setAttribute(REALM, context.getRealm())
                                .setAttribute(REPLAY, context.getAuthenticationSession().getAuthNote(REPLAY))
                                .setError("smsAuthCodeInvalid").createForm(TPL_CODE));
            } else {
                context.attempted();
            }
        }
    }

    private String manageCodeNotReceived(AuthenticationFlowContext context, String noCode, String enteredCode) {
        if ("not_received".equals(noCode) && Strings.isNullOrEmpty(enteredCode)) {
            int ttl = Integer.parseInt(context.getAuthenticatorConfig().getConfig().get("ttl"));
            String replay = context.getAuthenticationSession().getAuthNote(REPLAY);
            if (replay != null) {
                // expired after one resend tentative
                context.getAuthenticationSession().setAuthNote("ttl","0");
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        context.form().setAttribute(CUSTOM_PARAM, getPhoneNumberSuffix(context.getUser()))
                                .setAttribute(REALM, context.getRealm())
                                .setAttribute(REPLAY, context.getAuthenticationSession().getAuthNote(REPLAY))
                                .setError("smsAuthCodeExpired").createForm(TPL_CODE));
            } else {
                String lastCode = context.getAuthenticationSession().getAuthNote("code");
                context.getAuthenticationSession().setAuthNote(REPLAY, "True");
                sendSMS(context, context.getUser(), ttl, lastCode);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        context.form().setAttribute(CUSTOM_PARAM, getPhoneNumberSuffix(context.getUser()))
                                .setAttribute(REALM, context.getRealm())
                                .setAttribute(REPLAY, context.getAuthenticationSession().getAuthNote(REPLAY))
                                .setError("Code Renvoyé").createForm(TPL_CODE));
            }
            return null;
        }
        return enteredCode;
    }

    private String getPhoneNumberSuffix(UserModel user) {
        String numSuffix = "";
        String mobileNumberKeyclock = user.getFirstAttribute(MOBILE_NUMBER_ATTRIBUTE);
        if (!Strings.isNullOrEmpty(smsDevReceiver)) {
            mobileNumberKeyclock = smsDevReceiver;
        }
        if (mobileNumberKeyclock.length() >= 2) {
            StringBuilder sb = new StringBuilder(mobileNumberKeyclock);
            numSuffix = sb.substring(sb.length() - 2);
        }
        return numSuffix;
    }

    private long getMfaConfigTtl(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        long userMfaSessionTtl = 0L;
        try {
            int mfaSessionTtl = Integer.parseInt(config.getConfig().get("mfa_session_ttl"));
            userMfaSessionTtl = System.currentTimeMillis() + (long) mfaSessionTtl * 24 * 60 * 60 * 1000;
        } catch (NumberFormatException e) {
            LOG.error("Impossible de récupérer le ttl de la session mfa, la session ne sera pas enregistrée.");
        }
        return userMfaSessionTtl / 1000;
    }

    private void sendSMS(AuthenticationFlowContext context, UserModel user, int ttl, String code) {
        try {
            AuthenticatorConfigModel config = context.getAuthenticatorConfig();
            KeycloakSession session = context.getSession();
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String smsText = String.format(smsAuthText, code, Math.floorDiv(ttl, 60));
            String mobileNumberKeyclock = user.getFirstAttribute(MOBILE_NUMBER_ATTRIBUTE);
            // Check if a development number has been provided, then send sms to this number
            if (!Strings.isNullOrEmpty(smsDevReceiver)) {
                mobileNumberKeyclock = smsDevReceiver;
            }
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            authSession.setAuthNote("code", code);
            authSession.setAuthNote("ttl", Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
            NotificationServiceFactory.get(config.getConfig(), session).send(mobileNumberKeyclock, smsText);

            EventBuilder event = context.getEvent();
            event.clone()
                    .user(user)
                    .detail("auth_method", "sms-mfa")
                    .detail("msg", smsText)
                    .detail(MOBILE_NUMBER_ATTRIBUTE, mobileNumberKeyclock)
                    .storeImmediately(true)
                    .success();
        } catch (Exception e) {
            LOG.error(code, e);
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("smsAuthSmsNotSent", e.getMessage())
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    private String getDeviceIdFromCookie(AuthenticationFlowContext context) {
        Cookie cookieDeviceId = context.getHttpRequest().getHttpHeaders().getCookies().get("deviceId");
        return cookieDeviceId != null ? cookieDeviceId.getValue() : "";
    }

    private void storeMFASession(AuthenticationFlowContext context, int mfaSessionTtl) {
        UserModel user = context.getUser();

        String deviceId = UUID.randomUUID().toString();
        // Enregistrement du cookie
        HttpResponse originalResponse = ResteasyProviderFactory.getInstance().getContextData(HttpResponse.class);
        LOG.info("{}/realms/{}", context.getUriInfo().getBaseUri(), context.getRealm().getName());
        String cookiePath = context.getUriInfo().getBaseUri() + "/realms/" + context.getRealm().getName();
        originalResponse.addNewCookie(
                new NewCookie("deviceId", deviceId, cookiePath, null, null,
                        mfaSessionTtl, true, true));

        // Nettoyage des sessions invalides
        List<String> mfaActualValidSessions = user.getAttributeStream(MFA_SESSION_ATTRIBUTE)
                .filter(mfaSessionAsString -> isValidMfaSession(
                        parseMfaSession(mfaSessionAsString))).toList();
        List<String> allValidSessions = new ArrayList<>(mfaActualValidSessions);
        allValidSessions.add(formatMfaSession(Long.valueOf(mfaSessionTtl), deviceId));
        user.setAttribute(MFA_SESSION_ATTRIBUTE, allValidSessions);
        context.getUser().removeAttribute(PREVIOUS_MOBILE_ATTRIBUTE);
        user.removeRequiredAction("mobile-number-ra");
    }

    private Boolean isMFARequired(UserModel user, String deviceId) {
        Stream<String> mfaSessions = user.getAttributeStream(MFA_SESSION_ATTRIBUTE);
        List<MfaSession> mfaValidSessions = mfaSessions.map(this::parseMfaSession)
                .filter(mfaSession -> isValidMfaSessionForDevice(mfaSession, deviceId)).toList();
        return mfaValidSessions.isEmpty();
    }

    private MfaSession parseMfaSession(String mfaSessionString) {
        LOG.info(mfaSessionString);
        MfaSession mfaSession = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mfaSession = mapper.readValue(mfaSessionString, MfaSession.class);
        } catch (JsonProcessingException e) {
            LOG.error("Impossible de parser la session mfa: {0}", e);
        }
        return mfaSession;
    }

    private String formatMfaSession(Long ttl, String deviceId) {
        try {
            return String.format("{\"ttl\": %d, \"deviceId\": \"%s\"}", ttl * 1000, deviceId);
        } catch (Exception exception) {
            LOG.error("Invalid format for MFA session");
            return "<error>";
        }
    }

    private boolean isValidMfaSessionForDevice(MfaSession mfaSession, String deviceId) {
        if (mfaSession == null) {
            return false;
        }

        String sessionDeviceId = Objects.toString(mfaSession.getDeviceId(), "");
        long sessionTTL = Long.parseLong(Objects.toString(mfaSession.getTtl(), "0"));

        LOG.info("sessionDeviceId = {}, deviceId = {}", sessionDeviceId, deviceId);
        LOG.info("sessionTTL = {}, currentTimeMillis() = {}", sessionTTL, System.currentTimeMillis());

        return sessionDeviceId.equals(deviceId) && isValidMfaSession(mfaSession);
    }

    private boolean isValidMfaSession(MfaSession mfaSession) {
        if (mfaSession == null) {
            return false;
        }
        long sessionTTL = Long.parseLong(Objects.toString(mfaSession.getTtl(), "0"));
        return sessionTTL > System.currentTimeMillis();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getFirstAttribute(MOBILE_NUMBER_ATTRIBUTE) != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        if (user.getFirstAttribute(MOBILE_NUMBER_ATTRIBUTE) == null) {
            user.addRequiredAction("mobile-number-ra");
        }
    }

    @Override
    public void close() {
        LOG.info("Authentication action finalized");
    }
}
