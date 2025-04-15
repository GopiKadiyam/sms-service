package com.gk.sms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebEngageSMSData {
    private String toNumber;
    @JsonProperty("FromNumber")
    private String fromNumber1;
    @JsonProperty("fromNumber")
    private String fromNumber2;
    private String body;
}
