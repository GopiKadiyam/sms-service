package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterTemplateMsgReq {
    private String alias;
    private Map<String,String> data;
    private MtAdapterTemplateMsgMetadata meta;
    private MtAdapterTemplateMsgRecipient recipient;
}
