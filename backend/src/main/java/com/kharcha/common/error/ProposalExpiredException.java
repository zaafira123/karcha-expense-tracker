package com.kharcha.common.error;

public class ProposalExpiredException extends RuntimeException {
    public ProposalExpiredException(String message) {
        super(message);
    }
}
