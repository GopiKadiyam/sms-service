package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterMsgRes {
    private String charges;
    private String customid;
    private String customid1;
    private String id;
    private String iso_code;
    private int length;
    private String mobile;
    private String status;
    private String submitted_at;
    private int units;
}
