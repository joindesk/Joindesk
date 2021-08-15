package com.ariseontech.joindesk.exception;

import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class ErrorDetails {

    private Date timestamp;
    private String message;
    private ErrorCode error;
    private List<String> errors;

    public ErrorDetails(String message, ErrorCode error) {
        super();
        this.timestamp = new Timestamp(new Date().getTime());
        if (message != null) this.message = message;
        else this.message = error.getDetails();
        this.error = error;
    }
}
