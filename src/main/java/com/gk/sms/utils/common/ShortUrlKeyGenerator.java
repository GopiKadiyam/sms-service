package com.gk.sms.utils.common;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortUrlKeyGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LENGTH = 4;
    private static final SecureRandom random = new SecureRandom();

    public static String generateShortUrlKey() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

}
