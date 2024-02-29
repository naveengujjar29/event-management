package org.eventmanagement.exception;

import org.eventmanagement.controller.AuthController;
import org.eventmanagement.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = {AuthController.class})
@ResponseBody
public class ControllerExceptionHandler {


    @ExceptionHandler(UserAlreadyExistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    protected MessageResponse userAlreadyExistWithThisEmailId(final UserAlreadyExistException ex) {
        return new MessageResponse(ex.getMessage());
    }

}
