package com.gk.sms.utils.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class JsonUtil {

    private static final ObjectMapper objectMapper=new ObjectMapper();

    public static String convertObjectToJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON string", e);
        }
    }

}
