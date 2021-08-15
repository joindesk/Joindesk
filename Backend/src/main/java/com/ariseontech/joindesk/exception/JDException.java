package com.ariseontech.joindesk.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class JDException extends RuntimeException {

    private ErrorCode errorCode;

    private HttpStatus httpStatus;

    public JDException(String message, ErrorCode errorCode, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

}
