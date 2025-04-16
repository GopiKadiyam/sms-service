package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterTemplateMsgRespWrapper {

    private String status;
    private String message;
    private List<MtAdapterTemplateMsgResp> data;
}
