package com.gk.sms.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@Getter
@Setter
public class WebExchangeException extends RuntimeException{
    private String version;
    private String messageId;
    private String toNumber;
    private String status;
    private int statusCode;
    private String message;
    @JsonIgnore
    private HttpStatus httpStatus;

    public WebExchangeException(String version,String messageId,String toNumber,String status,int statusCode,String message,HttpStatus httpStatus){
        super(message);
        this.version=version;
        this.messageId=messageId;
        this.toNumber=toNumber;
        this.status=status;
        this.statusCode=statusCode;
        this.message=message;
        this.httpStatus=httpStatus;
    }
}
