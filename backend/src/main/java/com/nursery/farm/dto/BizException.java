package com.nursery.farm.dto;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
