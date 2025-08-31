package ru.mindils.jb2.app.exception;

public class InvalidJsonbException extends RuntimeException {

    public InvalidJsonbException(String message) {
        super(message);
    }

    public InvalidJsonbException(String message, Throwable cause) {
        super(message, cause);
    }
}