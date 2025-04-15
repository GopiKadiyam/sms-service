package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebEngageResponse {
    private String version;
    private String messageId;
    private String toNumber;
    private String status;
    private int statusCode;
    private String message;
}
