/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.action;

import java.util.function.Consumer;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spvie.oauth.provider.action.client.MesInfosService;
import com.spvie.oauth.provider.action.context.ApplicationMessages;
import com.spvie.oauth.provider.action.dto.DataProfil;
import com.spvie.oauth.provider.action.dto.RetourAction;
import com.spvie.oauth.provider.action.dto.Telephone;

public class MobileNumberRequiredAction implements RequiredActionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MobileNumberRequiredAction.class);

    private static final String PROVIDER_ID = "mobile-number-ra";
    private static final String NUMERO_INVALIDE = "invalidPhoneNumber";
    private static final String MOBILE_NUMBER = "mobile_number";
    private static final String PREVIOUS_MOBILE_NUMBER = "previous_mobile_number";
    private static final String TEL_MOBILE = "tel_mobile";
    private static final String USERNAME_FIELD = "user_name";
    private static final String ADHERENT_NUMBER_FIELD = "adherent_number";
    private static final String ADDRESS_EMAIL_FIELD = "email_address";
    private static final String BIRTHDATE_FIELD = "birth_date";
    private static final String INSEE_FIELD = "insee_number";
    private static final String DIAL_CODE = "dialCode";
    private static final String DATE_NAISSANCE_1 = "date_naissance_1";
    private static final String DATE_NAISSANCE_2 = "date_naissance_2";
    private static final String DATE_NAISSANCE_3 = "date_naissance_3";
    private static final String EMAIL_ADDRESS = "email_address";
    private static final String ADHERENT_NUMBER = "adherent_number";
    private static final String USER_NAME = "user_name";
    private static final String DATE_NAISS = "date_naiss";
    private static final String NUM_SS = "num_ss";
    private static final String CODE_TIERS_ATTRIBUT_KEYCLOAK = "code_tiers";

    private final String apiUrl;
    private final String apiKey;

    private final MesInfosService mesInfosService;

    public MobileNumberRequiredAction(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        mesInfosService = new MesInfosService(apiUrl, apiKey);
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getUser().getFirstAttribute(PREVIOUS_MOBILE_NUMBER) != null) {
            context.getUser().setSingleAttribute(MOBILE_NUMBER,
                    context.getUser().getFirstAttribute(PREVIOUS_MOBILE_NUMBER));
        }
        if (context.getUser().getFirstAttribute(MOBILE_NUMBER) == null) {
            context.getUser().addRequiredAction(PROVIDER_ID);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        // get telephone
        UserModel user = context.getUser();
        if (isAdherent(user)) {
            String codeTiers = user.getFirstAttribute(CODE_TIERS_ATTRIBUT_KEYCLOAK);
            ApplicationMessages messageLoader = new ApplicationMessages(context, Theme.Type.LOGIN);

            if (codeTiers == null && StringUtils.isEmpty(codeTiers)) {
                context.challenge(createForm(context, form -> form.addError(new FormMessage("codeTiers",
                        messageLoader.getMessage("error_count_not_completed")))));
                return;
            }

            StringBuilder finalNumber = new StringBuilder();
            autoLoadMobileNumber(user, finalNumber);

            if (finalNumber.isEmpty()) {
                //  show initial form
                context.challenge(createForm(context, null));
            }
        }
    }

    public void autoLoadMobileNumber(UserModel user, StringBuilder finalNumber) {
        if (isAdherent(user)) {
            String codeTiers = user.getFirstAttribute(CODE_TIERS_ATTRIBUT_KEYCLOAK);
            try {
                DataProfil dataProfil = mesInfosService.getDataProfil(codeTiers);
                if (StringUtils.isNotEmpty(dataProfil.getTelPE164())) {
                    finalNumber.append(dataProfil.getTelPE164().replace("+", "00").trim());
                } else {
                    Telephone telephone = new Telephone();
                    telephone.setTelNumero(dataProfil.getTelP());
                    telephone.setTelIndicatif(dataProfil.getIndicatifTelP());
                    if (StringUtils.isEmpty(telephone.getTelNumero())
                            || (("33".equalsIgnoreCase(telephone.getTelIndicatif())
                            || "+33".equalsIgnoreCase(telephone.getTelIndicatif())
                            || StringUtils.isEmpty(telephone.getTelIndicatif()))
                            && !"06".equals(telephone.getTelNumero().trim().substring(0, 2))
                            && !"07".equals(telephone.getTelNumero().trim().substring(0, 2)))) {
                        telephone.setTelNumero(dataProfil.getTelF());
                    }
                    if (StringUtils.isEmpty(telephone.getTelNumero())
                            || (("33".equalsIgnoreCase(telephone.getTelIndicatif())
                            || "+33".equalsIgnoreCase(telephone.getTelIndicatif())
                            || StringUtils.isEmpty(telephone.getTelIndicatif()))
                            && !"06".equals(telephone.getTelNumero().trim().substring(0, 2))
                            && !"07".equals(telephone.getTelNumero().trim().substring(0, 2)))) {
                        // show formulaire to update mobile number
                        return;
                    }
                    if (StringUtils.isNotEmpty(telephone.getTelNumero())) {
                        // update user with mobile number  and skip initial form
                        String mobileNumber = telephone.getTelNumero().trim().replaceAll("\\.", "");
                        if ("0".equals(mobileNumber.substring(0, 1))) {
                            mobileNumber = mobileNumber.substring(1);
                        }
                        if (StringUtils.isEmpty(telephone.getTelIndicatif().trim())) {
                            finalNumber.append("0033").append(mobileNumber);
                        } else {
                            String indicatif = telephone.getTelIndicatif().trim().replace("+", "00");
                            finalNumber.append(indicatif).append(mobileNumber);
                        }
                    }
                }
                user.setSingleAttribute(MOBILE_NUMBER, finalNumber.toString());
            } catch (Exception ex) {
                LOG.info("Erreur de validation du service Santelog Get UserProfile");
            }
            user.removeRequiredAction(PROVIDER_ID);
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // submitted form
        UserModel user = context.getUser();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        ApplicationMessages messageLoader = new ApplicationMessages(context, Theme.Type.LOGIN);
        String day = formData.getFirst(DATE_NAISSANCE_1);
        String month = formData.getFirst(DATE_NAISSANCE_2);
        String year = formData.getFirst(DATE_NAISSANCE_3);
        String userName = formData.getFirst(USER_NAME);
        String codeAdh = formData.getFirst(ADHERENT_NUMBER);

        if (!user.getFirstAttribute(CODE_TIERS_ATTRIBUT_KEYCLOAK).equals(codeAdh)) {
            context.challenge(createForm(context, form -> form.addError(
                    new FormMessage(ADHERENT_NUMBER, messageLoader.getMessage("error_count_not_completed")))));
            return;
        }

        if ((day == null || StringUtils.isEmpty(day)) || (month == null
                || StringUtils.isEmpty(month)) || (year == null
                || StringUtils.isEmpty(month))) {
            context.challenge(createForm(context, form -> form.addError(
                    new FormMessage(DATE_NAISS, messageLoader.getMessage("error_date_birthday")))));
            return;
        }

        if (userName == null && StringUtils.isEmpty(userName)) {
            context.challenge(createForm(context, form -> form.addError(
                    new FormMessage("Nom", messageLoader.getMessage("error_name")))));
            return;
        }

        RetourAction retourAction = updateProfil(formData, user);
        if (Boolean.FALSE.equals(retourAction.getSuccess())) {
            context.challenge(createForm(context, form -> form.addError(
                    new FormMessage("retour", retourAction.getMsgErreur()))));
            return;
        }

        context.challenge(createForm(context, form -> form.addSuccess(
                new FormMessage("retour", messageLoader.getMessage("success_validation_profil")))));
    }

    private RetourAction updateProfil(MultivaluedMap<String, String> formData, UserModel user) {
        String mobileNumberFinal = formData.getFirst(TEL_MOBILE);
        String mobileNumber = "";
        String countryCode = formData.getFirst(DIAL_CODE);
        StringBuilder finalNumber = new StringBuilder();
        if ("0".equals(mobileNumberFinal.substring(0, 1))) {
            mobileNumber = mobileNumberFinal.substring(1);
        }
        finalNumber.append("00").append(countryCode).append(mobileNumber);
        String day = formData.getFirst(DATE_NAISSANCE_1);
        String month = formData.getFirst(DATE_NAISSANCE_2);
        String year = formData.getFirst(DATE_NAISSANCE_3);

        StringBuilder numSs = new StringBuilder();
        numSs.append(formData.getFirst("num_ss_1")).append(formData.getFirst("num_ss_2"))
                .append(formData.getFirst("num_ss_3")).append(formData.getFirst("num_ss_4"))
                .append(formData.getFirst("num_ss_5")).append(formData.getFirst("num_ss_6"))
                .append(formData.getFirst("num_ss_7"));
        String emailAddress = formData.getFirst(EMAIL_ADDRESS);
        String adherentNumber = formData.getFirst(ADHERENT_NUMBER);
        String userName = formData.getFirst(USER_NAME);

        StringBuilder dateNaiss = new StringBuilder();
        dateNaiss.append(year).append("-").append(month).append("-").append(day);

        // validerProfilMfa

        JSONObject request = new JSONObject();
        request.put("cle_profilmfa", "12");
        request.put("codeAdherent", adherentNumber);
        request.put("codetiers", user.getFirstAttribute(CODE_TIERS_ATTRIBUT_KEYCLOAK));
        request.put("dateNaissance", dateNaiss);
        request.put("datemfa", "1990-12-13");
        request.put("email", emailAddress);
        request.put("indicatif", "+" + countryCode);
        request.put("nom", userName);
        request.put("numPortable", mobileNumberFinal);
        request.put("numinsee", numSs);
        request.put("utilisateur", "OAUTH-KEYCLOAK");

        //    Properties appProps = getProperties();

        //   apiUrl = appProps.getProperty("spring.application.apiUrl");
        //   apiKey = appProps.getProperty("spring.application.apiKey");

        RetourAction retourAction = mesInfosService.validerProfilMFA(request);

        if (Boolean.FALSE.equals(retourAction.getSuccess())) {
            return retourAction;
        }

        user.setSingleAttribute(MOBILE_NUMBER, finalNumber.toString());
        //  user.setSingleAttribute(NUM_SS, numSs.toString());
        user.setSingleAttribute(EMAIL_ADDRESS, emailAddress);
        user.setSingleAttribute(ADHERENT_NUMBER, adherentNumber);
        user.setSingleAttribute(USER_NAME, userName);
        // user.setSingleAttribute(DATE_NAISS, dateNaiss.toString());
        user.removeRequiredAction(PROVIDER_ID);
        return retourAction;
    }

    @Override
    public void close() {
        LOG.info("Required action committed");
    }

    // TODO : Change this logic for a more dynamic MFA approach (E.g - MFA group)
    private boolean isAdherent(UserModel user) {
        boolean isAdherent = user.getGroupsStream()
                .anyMatch(x -> x.getName().equalsIgnoreCase("USER_ADHERENT"));
        return isAdherent;
    }

    private Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formConsumer) {
        LoginFormsProvider form = context.form();
        form.setAttribute("username", context.getUser().getUsername());

        String mobileNumber = context.getUser().getFirstAttribute(MOBILE_NUMBER);

        form.setAttribute(MOBILE_NUMBER, mobileNumber == null ? "" : mobileNumber);

        if (formConsumer != null) {
            formConsumer.accept(form);
        }

        return form.createForm("update-mobile-number.ftl");
    }
}
