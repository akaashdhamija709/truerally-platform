package com.truerally.auth.exception;

public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException() {
        super("Account not verified. Please check your email.");
    }
}
