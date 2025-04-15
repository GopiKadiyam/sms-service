package com.gk.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterMsgResWrapper {
    private String message;
    private String group_id;
    private int status;
    private List<MtAdapterMsgRes> data;
}
