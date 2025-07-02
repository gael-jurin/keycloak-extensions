/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator.gateway;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.cm.text.MessagingClient;
import com.cm.text.models.Response;
import com.spvie.oauth.provider.authenticator.dto.SmsMessage;

public class NotificationClient extends MessagingClient {
    private final String productToken;
    private final String apiUrl;
    private final RestTemplate restTemplate;

    private final String apiKey;

    public NotificationClient(String productToken, String apiUrl, String apiKey) {
        super(productToken);
        this.productToken = productToken;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplateBuilder(rt-> rt.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("X-API-Key", apiKey);
                request.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                return execution.execute(request, body);
            })).build();
    }

    @Override
    public Response.HttpResponseBody sendTextMessage(String messageText, String from, String[] to) {
        SmsMessage message = new SmsMessage(productToken, from, to, messageText);
        initconverters();
        String result = restTemplate.postForObject(apiUrl, message, String.class);
        return getResponseBody(result);
    }

    private void initconverters() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);
    }
}
