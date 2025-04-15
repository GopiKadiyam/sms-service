package com.gk.sms.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlRequest {

    @NotBlank(message = "destinationUrl is mandatory")
    private String destinationUrl;
    private String token;
    private boolean activeFlag;
    private String comments;
    @NotBlank(message = "sender is mandatory")
    private String sender;

}
