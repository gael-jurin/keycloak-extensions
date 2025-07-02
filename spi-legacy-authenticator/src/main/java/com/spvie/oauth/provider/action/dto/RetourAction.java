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
import java.util.ArrayList;
import java.util.List;

public class RetourAction implements Serializable {
    private Boolean success;
    private int numErreur;
    private String msgErreur;
    private List<String> listErreur;

    public Boolean getSuccess() {
        return success;
    }
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    public int getNumErreur() {
        return numErreur;
    }
    public void setNumErreur(int numErreur) {
        this.numErreur = numErreur;
    }
    public String getMsgErreur() {
        return msgErreur;
    }
    public void setMsgErreur(String msgErreur) {
        this.msgErreur = msgErreur;
    }
    public List<String> getListErreur() {
        if (listErreur == null) {
            listErreur = new ArrayList<>();
        }
        return listErreur;
    }
    public void setListErreur(List<String> listErreur) {
        this.listErreur = listErreur;
    }
}
