package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterTemplateMsgResp {
    private String id;
    private String channel;
    private String from;
    private String to;
    private int credits;
    private String created_at;
    private String status;
    private String foreign_id;
}
