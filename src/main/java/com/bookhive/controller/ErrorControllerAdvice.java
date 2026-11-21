package com.bookhive.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ControllerAdvice(annotations = Controller.class)
public class ErrorControllerAdvice {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class,
        DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public String badRequest(Exception ex, Model model) {
        // Do not print database exception details to the browser.
        if (ex instanceof DataIntegrityViolationException || ex instanceof ObjectOptimisticLockingFailureException)
            model.addAttribute("error", "Concurrent change or conflicting data. Please retry.");
        else model.addAttribute("error", ex.getMessage());
        return "error/generic";
    }
}
