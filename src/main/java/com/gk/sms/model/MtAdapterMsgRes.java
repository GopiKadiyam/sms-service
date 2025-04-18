package com.gk.sms.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private Instant submitted_at;
    private int units;
}
