/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.event.logging;

import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class LogstashEventListenerProvider implements EventListenerProvider {
    private static final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final Logger log = lc.getLogger(LogstashEventListenerProvider.class.getName());

    @Override
    public void onEvent(Event event) {
        if (Strings.isNullOrEmpty(event.getError())) {
            if (log.isInfoEnabled()) {
                log.info(toLoggingEvent(event, Level.INFO).toString());
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn(toLoggingEvent(event, Level.WARN).toString());
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Customizing AdminEvent
    }

    @Override
    public void close() {
        // Closing the listener instance
    }

    private LoggingEvent toLoggingEvent(Event event, Level level) {
        LoggingEvent sb = new LoggingEvent(log.getName(), log, level, event.getError(),
                new Exception(),
                new Object[]{1});
        sb.getLoggerContextVO().getPropertyMap().put("type", event.getType().toString());
        sb.getLoggerContextVO().getPropertyMap().put("realmId", event.getRealmId());
        sb.getLoggerContextVO().getPropertyMap().put("clientId", event.getClientId());
        sb.getLoggerContextVO().getPropertyMap().put("userId", event.getUserId());
        sb.getLoggerContextVO().getPropertyMap().put("ipAddress", event.getIpAddress());

        if (event.getError() != null) {
            sb.getLoggerContextVO().getPropertyMap().put("error", event.getError());
        }

        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                sb.getLoggerContextVO().getPropertyMap().put(e.getKey(), e.getValue());
            }
        }
        return sb;
    }
}
