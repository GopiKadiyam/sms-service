package com.gk.sms.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gk.sms.config.converters.CustomWebEngageLocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebEngageMetadata {
    private String campaignType;
    //@JsonDeserialize(using = CustomWebEngageLocalDateTimeDeserializer.class)
    private String timestamp;
    private String messageId;
    private Map<String, String> custom;
    private WebEngageIndiaDLT indiaDLT;
}
