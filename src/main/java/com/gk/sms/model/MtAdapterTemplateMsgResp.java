package com.gk.sms.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtAdapterTemplateMsgResp {
    private String id;
    private String channel;
    private String from;
    private String to;
    private int credits;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private Instant created_at;
    private String status;
    private String foreign_id;
}
