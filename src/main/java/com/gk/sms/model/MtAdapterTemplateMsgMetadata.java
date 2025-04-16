package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterTemplateMsgMetadata {
    private List<String> tags;
    private String webhook_id;
    private String foreign_id;
    private String service;
    private int flash;
}
