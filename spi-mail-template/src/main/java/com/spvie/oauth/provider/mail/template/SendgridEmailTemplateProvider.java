/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.template;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.LinkExpirationFormatterMethod;
import org.keycloak.theme.beans.MessageFormatterMethod;

import com.google.common.base.Strings;

/**
 * Based on org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider
 */
public class SendgridEmailTemplateProvider implements EmailTemplateProvider {

    protected KeycloakSession session;
    /**
     * authenticationSession can be null for some email sendings,
     * it is filled only for email sendings performed as part of the authentication
     * session (email verification, password reset, broker link
     * etc.)!
     */
    protected AuthenticationSessionModel authenticationSession;
    protected RealmModel realm;
    protected UserModel user;
    protected final Map<String, Object> attributes = new HashMap<>();

    private String pwdResetTempId;
    private String emailVerifTempId;
    private String exeActionsTempId;
    private EmailSenderProvider emailSender;

    public SendgridEmailTemplateProvider(KeycloakSession session,
                                         String pwdResetTempId,
                                         String emailVerifTempId,
                                         String exeActionsTempId) {
        this.session = session;
        this.pwdResetTempId = pwdResetTempId;
        this.emailVerifTempId = emailVerifTempId;
        this.exeActionsTempId = exeActionsTempId;
        this.emailSender = session.getProvider(EmailSenderProvider.class);
    }

    @Override
    public EmailTemplateProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public EmailTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public EmailTemplateProvider setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
        return this;
    }

    protected String getRealmName() {
        if (realm.getDisplayName() != null) {
            return realm.getDisplayName();
        } else {
            return ObjectUtil.capitalize(realm.getName());
        }
    }

    @Override
    public void sendEvent(Event event) throws EmailException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("event", new EventBean(event));

        send(toCamelCase(event.getType()) + "Subject",
                "event-" + event.getType().toString().toLowerCase() + ".ftl", attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realmName", getRealmName());

        if (Strings.isNullOrEmpty(pwdResetTempId)) {
            send("passwordResetSubject", "password-reset.ftl", attributes);
        } else {
            EmailTemplate email = processTemplate("passwordResetSubject", Collections.emptyList(),
                    "password-reset.ftl", attributes);
            JSONObject templateBody = new JSONObject();
            templateBody.put("url",  link);
            emailSender.send(realm.getSmtpConfig(), user, email.getSubject(),
                    pwdResetTempId, templateBody.toString());
        }
    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {
        setRealm(session.getContext().getRealm());
        setUser(user);

        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        attributes.put("realmName", realm.getName());

        // Example of new variable
        attributes.put("testNewVariable", "Say hello to my little friend");

        EmailTemplate email = processTemplate("emailTestSubject", Collections.emptyList(),
                "email-test.ftl", attributes);
        send(config, email.getSubject(), email.getTextBody(), email.getHtmlBody());
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realmName", getRealmName());

        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes
                .get(IDENTITY_PROVIDER_BROKER_CONTEXT);
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        idpAlias = ObjectUtil.capitalize(idpAlias);

        attributes.put("identityProviderContext", brokerContext);
        attributes.put("identityProviderAlias", idpAlias);

        List<Object> subjectAttrs = Arrays.asList(idpAlias);
        send("identityProviderLinkSubject", subjectAttrs,
                "identity-provider-link.ftl", attributes);
    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realmName", getRealmName());

        if (Strings.isNullOrEmpty(exeActionsTempId)) {
            send("executeActionsSubject", "executeActions.ftl", attributes);
        } else {
            EmailTemplate email = processTemplate("executeActionsSubject", Collections.emptyList(),
                    "executeActions.ftl", attributes);
            JSONObject templateBody = new JSONObject();
            templateBody.put("url",  link);
            // templateBody.put("realmName", getRealmName());
            emailSender.send(realm.getSmtpConfig(), user, email.getSubject(),
                    exeActionsTempId, templateBody.toString());
        }
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realmName", getRealmName());

        if (Strings.isNullOrEmpty(emailVerifTempId)) {
            send("emailVerificationSubject", "email-verification.ftl", attributes);
        } else {
            EmailTemplate email = processTemplate("emailVerificationSubject", Collections.emptyList(),
                    "email-verification.ftl", attributes);
            JSONObject templateBody = new JSONObject();
            templateBody.put("url",  link);
            // templateBody.put("realmName", getRealmName());
            emailSender.send(realm.getSmtpConfig(), user, email.getSubject(),
                    emailVerifTempId, templateBody.toString());
        }
    }

    @Override
    public void sendEmailUpdateConfirmation(String link, long expirationInMinutes, String address)
            throws EmailException {
    }

    /**
     * Add link info into template attributes.
     *
     * @param link                to add
     * @param expirationInMinutes to add
     * @param attributes          to add link info into
     */
    protected void addLinkInfoIntoAttributes(String link, long expirationInMinutes, Map<String,
            Object> attributes) throws EmailException {
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        try {
            Locale locale = session.getContext().resolveLocale(user);
            attributes.put("linkExpirationFormatter", new LinkExpirationFormatterMethod(getTheme()
                    .getMessages(locale), locale));
            attributes.put("url", new UrlBean(realm, getTheme(), baseUri, null));
        } catch (IOException e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes)
            throws EmailException {
        send(subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes);
    }

    protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes,
                                            String template, Map<String, Object> attributes) throws EmailException {
        try {
            Theme theme = getTheme();
            Locale locale = session.getContext().resolveLocale(user);
            attributes.put("locale", locale);
            Properties rb = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            attributes.put("properties", theme.getProperties());
            String subject = new MessageFormat(rb.getProperty(subjectKey, subjectKey),
                    locale).format(subjectAttributes.toArray());
            String htmlBody = "";
            return new EmailTemplate(subject, "", htmlBody);
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.EMAIL);
    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate,
                     Map<String, Object> bodyAttributes) throws EmailException {
        try {
            EmailTemplate email = processTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
            send(email.getSubject(), "", "<html><br>" + bodyAttributes.get("link") + "</br></html>");
        } catch (EmailException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    protected void send(String subject, String textBody, String htmlBody) throws EmailException {
        send(realm.getSmtpConfig(), subject, textBody, htmlBody);
    }

    protected void send(Map<String, String> config, String subject, String textBody, String htmlBody)
            throws EmailException {
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
        emailSender.send(config, user, subject, textBody, htmlBody);
    }

    @Override
    public void close() {
    }

    protected String toCamelCase(EventType event) {
        StringBuilder sb = new StringBuilder("event");
        for (String s : event.name().toLowerCase().split("_")) {
            sb.append(ObjectUtil.capitalize(s));
        }
        return sb.toString();
    }

    protected class EmailTemplate {

        private String subject;
        private String textBody;
        private String htmlBody;

        public EmailTemplate(String subject, String textBody, String htmlBody) {
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
        }

        public String getSubject() {
            return subject;
        }

        public String getTextBody() {
            return textBody;
        }

        public String getHtmlBody() {
            return htmlBody;
        }
    }

}
