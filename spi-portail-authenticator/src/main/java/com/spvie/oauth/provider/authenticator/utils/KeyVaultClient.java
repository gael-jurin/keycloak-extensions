/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.authenticator.utils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

public class KeyVaultClient {
    private static final Logger log = LoggerFactory.getLogger(KeyVaultClient.class);

    public Map<String, String> loadPortalSecretFromVault(String[] clientRoles) {

        Map<String, String> secrets = new HashMap<>();
        // Replace with your Key Vault URL
        String keyVaultUrl = "https://<our-key-vault-name>.vault.azure.net/";

        // Create a SecretClient using DefaultAzureCredential
        SecretClient secretClient = new SecretClientBuilder().vaultUrl(keyVaultUrl).credential(
                new DefaultAzureCredentialBuilder().build()).buildClient();

        try {
            for (String clientRole : clientRoles) {
                KeyVaultSecret secret = secretClient.getSecret(clientRole);
                secrets.put(secret.getName(), secret.getValue());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return secrets;
    }
}

