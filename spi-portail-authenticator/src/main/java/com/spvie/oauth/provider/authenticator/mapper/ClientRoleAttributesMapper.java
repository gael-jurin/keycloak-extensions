/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientRoleAttributesMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper,
        UserInfoTokenMapper {

    public static final String PROVIDER_ID = "client-role-attributes-mapper";
    public static final String PORTAL_PWD = "portal_pwd";

    private static final Logger log = LoggerFactory.getLogger(ClientRoleAttributesMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ClientRoleAttributesMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Client Role Attributes Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds custom attributes of client role assignments to the token.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                            UserSessionModel userSession, KeycloakSession session,
                            ClientSessionContext clientSessionCtx) {

        UserModel user = userSession.getUser();
        ClientModel client = clientSessionCtx.getClientSession().getClient();
        Set<RoleModel> userRoles = user.getClientRoleMappingsStream(client).collect(Collectors.toSet());

        user.getGroupsStream().forEach(group -> {
                if (group.getClientRoleMappingsStream(client).count() > 0) {
                    userRoles.addAll(group.getClientRoleMappingsStream(client).collect(Collectors.toSet()));
                }
            });

        Map<String, Map<String, String>> roleAttributesMap = new HashMap<>();

        for (RoleModel role : userRoles) {
            Map<String, List<String>> attrs = role.getAttributes();
            Map<String, String> flatAttrs = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
                /* Todo: Reactivate when we will want to run with symetric encryption */

                /* if (!entry.getValue().isEmpty() && PORTAL_PWD.equals(entry.getKey())) {
                    try {
                        flatAttrs.put(entry.getKey(), AESProvider.encrypt(entry.getValue().get(0),
                                userSession.getId()));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                } else {

                } */

                flatAttrs.put(entry.getKey(), entry.getValue().isEmpty() ? null : entry.getValue().get(0));
            }

            roleAttributesMap.put(role.getName(), flatAttrs);
        }

        // Add to token
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, roleAttributesMap);
    }
}

