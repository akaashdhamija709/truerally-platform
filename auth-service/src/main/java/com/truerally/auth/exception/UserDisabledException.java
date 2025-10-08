package com.truerally.auth.exception;

public class UserDisabledException extends RuntimeException {
    public UserDisabledException(String message) { super(message); }
}
