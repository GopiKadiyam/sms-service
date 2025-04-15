package com.gk.sms.model;

import com.gk.sms.utils.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWiseApiKey {
    private ServiceType serviceType;
    private String smsc;
    private String userId;
}
