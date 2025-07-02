/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsMessage {

    @JsonProperty("SmsReference")
    String smsReference;
    @JsonProperty("From")
    String from;
    @JsonProperty("To")
    String[] to;
    @JsonProperty("Message")
    String message;

    public SmsMessage(String smsReference, String from, String[] to, String message) {
        this.smsReference = smsReference;
        this.from = from;
        this.to = to;
        this.message = message;
    }
}
