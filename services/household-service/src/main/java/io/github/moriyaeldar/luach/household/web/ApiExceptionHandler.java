package io.github.moriyaeldar.luach.household.web;

import io.github.moriyaeldar.luach.household.service.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Errors.NotFound.class)
    ProblemDetail notFound(Errors.NotFound e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(Errors.Forbidden.class)
    ProblemDetail forbidden(Errors.Forbidden e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(Errors.Invalid.class)
    ProblemDetail invalid(Errors.Invalid e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Errors.Gone.class)
    ProblemDetail gone(Errors.Gone e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage());
    }
}
