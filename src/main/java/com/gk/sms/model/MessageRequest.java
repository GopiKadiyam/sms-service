package com.gk.sms.model;


import com.gk.sms.utils.enums.CRMType;
import com.gk.sms.utils.enums.Country;
import com.gk.sms.utils.enums.KafkaMsgType;
import com.gk.sms.utils.enums.MessageType;
import com.gk.sms.utils.enums.MsgStatus;
import com.gk.sms.utils.enums.ServiceType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest{
    private String msgId;
    @NotNull(message = "'country' field is mandatory and cannot be null or empty. The value should be in IN,INTL ")
    private Country country;
    @NotNull(message = "'serviceType' mandatory and cannot be null or empty.The value should be in TRANS,PROMO,OTP")
    private ServiceType serviceType;
    @NotEmpty(message = "'from' field is mandatory and cannot be null or empty")
    private String from;
    @NotEmpty(message = "'to' field is mandatory and cannot be null or empty")
    private String to;
    @NotEmpty(message = "'body' field is mandatory and cannot be null or empty")
    private String body;
    @NotBlank(message = "'templateId' is mandatory and cannot be null or empty")
    @Size(min = 19, max = 19, message = "templateId must be exactly 19 characters")
    private String templateId;
    @NotBlank(message = "'entityId' is mandatory and cannot be null or empty")
    @Size(min = 19, max = 19, message = "entityId must be exactly 19 characters")
    private String entityId;
    @NotNull(message = "'messageType' mandatory and cannot be null or empty.The value should be in U,N,A ")
    private MessageType messageType;
    private String customId;
    private Map<String, String> metadata;
    private boolean flash;
    private String webhookId;
    private String tenantId;
    private String crmMsgId;
    private String webEngageVersion;
    private CRMType crmMsgType;
    private LocalDateTime smsSentOn;

    private KafkaMsgType kafkaMsgType;
    private MsgStatus msgStatus;

    private String statusJson;
    private String type;
    private String pId;
    private String smscId;
    private String tm;
    private String dlrUrl;
}
