package com.ariseontech.joindesk.exception;

import org.json.JSONArray;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes"})
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Object> handleAllExceptions(Exception ex) {
        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());
        ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.BAD_REQUEST);
        error.setErrors(details);
        ex.printStackTrace();
        return new ResponseEntity(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public final ResponseEntity<Object> handleAccessDeniedExceptions(Exception ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());
        ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.BAD_REQUEST);
        error.setErrors(details);
        ex.printStackTrace();
        return new ResponseEntity(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<Object> constraintViolationException(Exception ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());
        ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.BAD_REQUEST);
        error.setErrors(details);
        ex.printStackTrace();
        return new ResponseEntity(error, HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(JDException.class)
    public final ResponseEntity<Object> handlePreconditionFailedException(JDException ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        if (ObjectUtils.isEmpty(ex.getLocalizedMessage()))
            Optional.ofNullable(ex.getErrorCode().getDetails()).ifPresent(details::add);
        Optional.ofNullable(ex.getLocalizedMessage()).ifPresent(details::add);
        ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ex.getErrorCode());
        error.setErrors(details);
        if (!ex.getErrorCode().equals(ErrorCode.VALIDATION_FAILED)) ex.printStackTrace();
        else logger.info(ex.getMessage());
        return new ResponseEntity(error, ex.getHttpStatus());
    }

    @ExceptionHandler(JDTransitionException.class)
    public final ResponseEntity<Object> handleRequiredTransitionFailedException(JDTransitionException ex, WebRequest request) {
        return new ResponseEntity(ex.getMessage(), ex.getHttpStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<String> details = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            details.add(error.getDefaultMessage());
        }
        ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED);
        error.setErrors(details);
        ex.printStackTrace();
        return new ResponseEntity(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public final ResponseEntity<Object> invalidDataAccess(Exception ex) {
        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());
        ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.BAD_REQUEST);
        error.setErrors(details);
        if (ex.getCause().getCause() != null && ex.getCause().getCause().getMessage().contains("does not exist")) {
            details = new ArrayList<>();
            error = new ErrorDetails("Invalid Tenant", ErrorCode.BAD_REQUEST);
            error.setErrors(details);
            return new ResponseEntity(error, HttpStatus.NOT_FOUND);
        } else {
            ex.printStackTrace();
            return new ResponseEntity(error, HttpStatus.BAD_REQUEST);
        }
    }
}