package com.gk.sms.config.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class CustomWebEngageLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
        String date = p.getText();
        OffsetDateTime odt = OffsetDateTime.parse(date, formatter);
        return odt.toLocalDateTime(); // discard the zone offset if needed
    }
}
