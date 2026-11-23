package com.kharcha.common.error;

public class UndoWindowClosedException extends RuntimeException {
    public UndoWindowClosedException(String message) {
        super(message);
    }
}
