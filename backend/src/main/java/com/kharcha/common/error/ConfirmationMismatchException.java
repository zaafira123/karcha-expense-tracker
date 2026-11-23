package com.kharcha.common.error;

public class ConfirmationMismatchException extends RuntimeException {
    public ConfirmationMismatchException() {
        super("The email does not match this account");
    }
}
