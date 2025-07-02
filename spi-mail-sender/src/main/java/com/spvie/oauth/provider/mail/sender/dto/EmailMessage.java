/*
 * Copyright (c) 2023 by Spvie Technology, Inc., All rights reserved.
 * This source code, and resulting software, is the confidential and proprietary information
 * ("Proprietary Information") and is the intellectual property ("Intellectual Property")
 * of Spvie Technology, Inc. ("The Company"). You shall not disclose such Proprietary Information and
 * shall use it only in accordance with the terms and conditions of any and all license
 * agreements you have entered into with The Company.
 */

package com.spvie.oauth.provider.mail.sender.dto;

import java.util.Objects;

import org.json.JSONObject;

public class EmailMessage {
    private JSONObject msg;

    public EmailMessage(Builder builder) {
        this.msg = builder.message;
    }

    public JSONObject getMsg() {
        return msg;
    }

    public static class Builder {
        public static final String TEMPLATE_ID = "id";
        public static final String ADDRESS = "address";

        private JSONObject from;
        private JSONObject tos;
        private JSONObject reply;
        private JSONObject template;
        private JSONObject data;
        private String subject;
        private String textBody;
        private JSONObject message;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder withFrom(String sender) {
            this.from = new JSONObject();
            this.from.put(ADDRESS, sender);
            return this;
        }

        public Builder withTos(String receiver) {
            this.tos = new JSONObject();
            this.tos.put(ADDRESS, receiver);
            return this;
        }

        public Builder withReplyTo(String replyTo) {
            this.reply = new JSONObject();
            this.reply.put(ADDRESS, replyTo);
            return this;
        }

        public Builder withTemplate(String template) {
            this.template = new JSONObject();
            this.template.put(TEMPLATE_ID, template);
            return this;
        }

        public Builder withData(JSONObject data) {
            this.data = data;
            return this;
        }

        public Builder withTextBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        public EmailMessage build() {
            message = new JSONObject();
            message.put("Ccs", new String[0]);
            message.put("Bccs", new String[0]);
            message.put("ReplyTo", reply);
            message.put("Subject", subject);
            message.append("to", tos);
            message.putOpt("from", from);
            if (Objects.nonNull(template) && Objects.nonNull(data)) {
                template.put("data", data);
                message.put("template", template);
            } else {
                message.put("Body", textBody);
            }
            return new EmailMessage(this);
        }
    }
}
