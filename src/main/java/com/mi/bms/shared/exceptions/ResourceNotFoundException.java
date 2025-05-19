package com.mi.bms.shared.exceptions;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource) {
        super("NOT_FOUND", String.format("%s not found", resource));
    }

    public ResourceNotFoundException(String resource, String id) {
        super("NOT_FOUND", String.format("%s with id %s not found", resource, id));
    }
}