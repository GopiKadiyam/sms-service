package com.gk.sms.utils.enums;

import java.util.Arrays;

public enum MessageType {
    U("U",2),N("N",0),A("A",0);
    private final String value;
    private final int kennelValue;
    MessageType(String value,int kennelValue){
        this.value=value;
        this.kennelValue=kennelValue;
    }
    public String getValue() {
        return this.value;
    }
    public int getKennelValue() {
        return this.kennelValue;
    }
    public static MessageType fromValue(String code) {
        return Arrays.stream(MessageType.values())
                .filter(status -> status.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
