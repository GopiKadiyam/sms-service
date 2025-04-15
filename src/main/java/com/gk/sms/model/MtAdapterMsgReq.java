package com.gk.sms.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterMsgReq {
    private String sender;
    private List<String> to;
    private String message;
    private String service;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    private OffsetDateTime time;
    private String type;
    private int flash;
    private String custom;
    private String port;
    private String entity_id;
    private String template_id;
    private int max_units;
    private String webhook_id;
    private Map<String,String> meta;
}
