package io.github.moriyaeldar.luach.tasks.service;

/** A request that is well-formed but breaks a business rule, e.g. a FIXED task without a time. */
public class InvalidTaskException extends RuntimeException {
    public InvalidTaskException(String message) {
        super(message);
    }
}
