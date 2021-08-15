package com.ariseontech.joindesk.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class JDTransitionException extends RuntimeException {

    private ErrorCode errorCode;

    private String response;

    private HttpStatus httpStatus;

    public JDTransitionException(String response, ErrorCode errorCode, HttpStatus httpStatus) {
        super(response);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

}
