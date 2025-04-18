package com.gk.sms.config.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class CustomWebEngageLocalDateTimeDeserializer extends JsonDeserializer<Instant> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
        String date = p.getText();
        OffsetDateTime odt = OffsetDateTime.parse(date, formatter);
        return odt.toInstant(); // discard the zone offset if needed
    }
}
