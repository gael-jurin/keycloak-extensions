/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.action.client;

import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.spvie.oauth.provider.action.dto.DataProfil;
import com.spvie.oauth.provider.action.dto.RetourAction;
import com.spvie.oauth.provider.action.dto.Telephone;

public class MesInfosService {
    private static final String VALIDER_PROFIL_MFA = "/validerProfilMFA";
    private static final String GET_TELEPHONE_PORTABLE = "/getTelephonePortable?codeTiers=";

    private static final String GET_DATA_PROFIL = "/getDataProfil?codeTiers=";

    private StringBuilder apiUrl;
    private RestTemplate restTemplate;
    private String apiKey;

    public MesInfosService(String apiUrl, String apiKey) {
        this.apiUrl = new StringBuilder(apiUrl);
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplateBuilder(rt-> rt.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("X-API-Key", apiKey);
                request.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                return execution.execute(request, body);
            })).build();
    }

    public RetourAction validerProfilMFA(JSONObject request) {
        return restTemplate.postForObject(apiUrl.append(VALIDER_PROFIL_MFA).toString(), request.toString(),
                RetourAction.class);
    }

    public List<Telephone> getTelephonePortable(String codeTierss) {
        ResponseEntity<Telephone[]> responseEntity = restTemplate.getForEntity(apiUrl.append(GET_TELEPHONE_PORTABLE)
                .append(codeTierss).toString(), Telephone[].class);
        if (responseEntity.getBody() == null) {
            return null;
        }
        return Arrays.asList(responseEntity.getBody());
    }

    public DataProfil getDataProfil(String codeTierss) {
        ResponseEntity<DataProfil> responseEntity = restTemplate.getForEntity(apiUrl.append(GET_DATA_PROFIL)
                .append(codeTierss).toString(), DataProfil.class);
        if (responseEntity.getBody() == null) {
            return null;
        }
        return responseEntity.getBody();
    }
}
