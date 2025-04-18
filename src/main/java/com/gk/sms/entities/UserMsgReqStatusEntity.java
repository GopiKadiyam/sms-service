package com.gk.sms.entities;

import com.gk.sms.utils.enums.MsgStatus;
import com.gk.sms.utils.enums.SMSStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_msg_req_status")
public class UserMsgReqStatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "msg_id", nullable = false)
    private UserMsgReqEntity userMessage;
    private String msgGroupId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    private Instant smsSentOn;
    private Instant  dlrSentOn;
    private Instant  dlrDeliveredOn;
    @Enumerated(EnumType.STRING)
    private SMSStatus dlrStatus;
    private String dlrStatusCode;
    private String dlrStatusDescription;
    @Enumerated(EnumType.STRING)
    private MsgStatus msgStatus;
}
