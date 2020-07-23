/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/9 15:32
 */

package com.xxxxx.xxxxxxxx.project.controllers;

import com.xxxxx.xxxxxxxx.project.models.exception.StaticException;
import com.xxxxx.xxxxxxxx.project.models.Protocol.SetAttrValueRequest;
import com.xxxxx.xxxxxxxx.project.models.Protocol.SetAttrValueResponse;
import com.xxxxx.xxxxxxxx.project.models.RequestContext;
import com.xxxxx.xxxxxxxx.project.services.PMService;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import javax.validation.Valid;

import javax.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

@ControllerAdvice
@RestController
@RequestMapping(value = "/xxxxxxxx/api/project/v1")
public class PMController {
    private final Logger rootLogger = LoggerFactory.getLogger("");
    private final PMService service;

    @Getter(AccessLevel.PUBLIC) private static final String kAuth = "auth";

    @Autowired
    public PMController(PMService service) {
        this.service = service;
    }

    @Autowired
    RequestContext context;

    @RequestMapping (method = {RequestMethod.PUT, RequestMethod.POST},
            value = {"/{type:path1|path2|path3}"}, headers = {"X-KEY=Value"})
    public ResponseEntity setValue(final HttpServletRequest orinRequest,
                                   @Valid @RequestBody SetAttrValueRequest request,
                                   @PathVariable String type,
                                   @RequestAttribute(kAuth) AuthInfo authInfo)
            throws StaticException, AuthenticationException {
        String objectType = StringUtils.isEmpty(type) ? "path1" : type;
        context.init(request, new SetAttrValueResponse(), objectType);
        service.setValue(HttpMethod.valueOf(orinRequest.getMethod()));

        return context.buildResponseEntity();
    }

    private String extractFieldErrorMessage(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(";", "Field validation error: ", ""));
    }

    // Most outside exception handler, handle the errors related to the API protocol
    // and any errors not caught by the service.
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity controllerFailedResponse(NativeWebRequest request, Exception e) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        Response errResponse = new Response(StatusCode.InternalError);

        if (AuthenticationException.class.equals(e.getClass())) {
            httpStatus = HttpStatus.UNAUTHORIZED;
            errResponse.setStatus(StatusCode.Unauthorized, e.getMessage());

        } else if (HttpMediaTypeNotSupportedException.class.equals(e.getClass())) {
            httpStatus = HttpStatus.NOT_FOUND;
            errResponse.setStatus(StatusCode.NotSupported,"Not supported Content-Type in Header.");

        } else if (HttpRequestMethodNotSupportedException.class.equals(e.getClass())) {
            httpStatus = HttpStatus.NOT_FOUND;
            errResponse.setStatus(StatusCode.NotSupported, "Not supported HTTP method.");

        } else if (MissingPathVariableException.class.equals(e.getClass())) {
            httpStatus = HttpStatus.NOT_FOUND;
            errResponse.setStatus(StatusCode.BadURI, String.format("Missing path variable: %s",
                    ((MissingPathVariableException) e).getVariableName()));

        } else if (HttpMessageNotReadableException.class.equals(e.getClass())) {
            httpStatus = HttpStatus.BAD_REQUEST;
            errResponse.setStatus(StatusCode.Corrupted, "Unresolved request body.");

        } else if (MethodArgumentNotValidException.class.equals(e.getClass())) {
            httpStatus = HttpStatus.BAD_REQUEST;
            errResponse.setStatus(StatusCode.InvalidArgument, extractFieldErrorMessage(
                    (MethodArgumentNotValidException) e));

        } else {
            rootLogger.error("Caught exception in controller: {}", e.getMessage(), e);
        }
        return ResponseEntity.status(httpStatus).body(errResponse);
    }
}
