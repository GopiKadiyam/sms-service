package com.gk.sms.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_wise_service_permissions")
public class UserWiseServicePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;
    @ManyToOne
    @JoinColumn(name = "service_type_id", nullable = false)
    private MsgServiceTypeEntity serviceType;
    @ManyToOne
    @JoinColumn(name = "smsc_id", nullable = false)
    private SMSCEntity smsc;

}
