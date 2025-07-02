/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.sender.gateway;

import org.json.JSONObject;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class MailNotificationClient {
    private final String apiUrl;
    private final RestTemplate restTemplate;

    private final String apiKey;

    public MailNotificationClient(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplateBuilder(rt-> rt.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("X-API-Key", apiKey);
                request.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                return execution.execute(request, body);
            })).build();
    }

    public String sendEmailMessage(JSONObject request) {
        return restTemplate.postForObject(apiUrl, request.toString(), String.class);
    }
}
