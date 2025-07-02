/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.action.dto;

import java.io.Serializable;

public class Telephone implements Serializable {

    private String codetiers;
    private String codeadresse;
    private String telNumero;
    private String telIndicatif;
    private String telIdentite;
    private String telProfession;
    private String telService;
    private String telType;

    public String getCodetiers() {
        return codetiers;
    }
    public void setCodetiers(String codetiers) {
        this.codetiers = codetiers;
    }
    public String getCodeadresse() {
        return codeadresse;
    }
    public void setCodeadresse(String codeadresse) {
        this.codeadresse = codeadresse;
    }
    public String getTelNumero() {
        return telNumero;
    }
    public void setTelNumero(String telNumero) {
        this.telNumero = telNumero;
    }
    public String getTelIndicatif() {
        return telIndicatif;
    }
    public void setTelIndicatif(String telIndicatif) {
        this.telIndicatif = telIndicatif;
    }
    public String getTelIdentite() {
        return telIdentite;
    }
    public void setTelIdentite(String telIdentite) {
        this.telIdentite = telIdentite;
    }
    public String getTelProfession() {
        return telProfession;
    }
    public void setTelProfession(String telProfession) {
        this.telProfession = telProfession;
    }
    public String getTelService() {
        return telService;
    }
    public void setTelService(String telService) {
        this.telService = telService;
    }
    public String getTelType() {
        return telType;
    }
    public void setTelType(String telType) {
        this.telType = telType;
    }

}
