package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebEngageSMSRequest {
    private String version;
    private WebEngageSMSData smsData;
    private WebEngageMetadata metadata;
}
