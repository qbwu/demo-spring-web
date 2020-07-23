/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/12 17:49
 */

package com.xxxxx.xxxxxxxx.project.models;

import com.xxxxx.xxxxxxxx.project.models.Protocol.Request;
import com.xxxxx.xxxxxxxx.project.models.Protocol.Response;
import com.xxxxx.xxxxxxxx.project.models.Protocol.ResponseData;
import com.xxxxx.xxxxxxxx.project.models.Protocol.StatusCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class RequestContext<T1 extends Request, T2 extends ResponseData> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private T1 request;
    private Response response;
    private HttpStatus httpStatus;

    @Getter(AccessLevel.PUBLIC)
    private String templateType;

    public void init(T1 request, T2 response, String objectType) {
        this.httpStatus = HttpStatus.OK;
        this.request = request;
        this.response = new Response().withBody(response);
        this.templateType = objectType;
    }

    public ResponseEntity buildResponseEntity() {
        return ResponseEntity.status(httpStatus).body(response);
    }

    @Synchronized
    public Boolean isStatusOK() {
        return httpStatus == HttpStatus.OK
                && this.response.isStatus(StatusCode.OK);
    }

    @Synchronized
    public void setStatus(HttpStatus httpStatus, StatusCode code) {
        this.httpStatus = httpStatus;
        this.response.setStatus(code);
    }

    @Synchronized
    public void setStatus(HttpStatus httpStatus,
                          StatusCode code, String cause) {
        this.httpStatus = httpStatus;
        this.response.setStatus(code, cause);
    }

    public T1 getRequest() { return request; }
    public T2 getResponse() { return (T2) response.getData(); }
}
