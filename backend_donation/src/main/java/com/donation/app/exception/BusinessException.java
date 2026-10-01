package com.donation.app.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter 
public class BusinessException extends RuntimeException{
    private static final long serialVersionUID=1L;
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message){
        super(message);
        this.status=status;
    }
}
