package com.gk.sms.utils.enums;

import java.util.Arrays;

public enum CRMType {
    WEB_ENGAGE, MO_ENGAGE;

    public static CRMType fromValue(String code) {
        return Arrays.stream(CRMType.values())
                .filter(status -> status.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
