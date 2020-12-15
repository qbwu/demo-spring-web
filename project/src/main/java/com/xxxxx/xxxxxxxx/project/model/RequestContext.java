/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/12 17:49
 */

package com.xxxxx.xxxxxxxx.project.model;

import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.enums.ServiceScope;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.xxxxx.xxxxxxxx.project.model.Protocol.Request;
import com.xxxxx.xxxxxxxx.project.model.Protocol.Response;
import com.xxxxx.xxxxxxxx.project.model.Protocol.ResponseData;
import com.xxxxx.xxxxxxxx.project.model.Protocol.StatusCode;
import org.springframework.stereotype.Component;


@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class RequestContext<T1 extends Request, T2 extends ResponseData> {
    private T1 request;
    private Response response;
    private HttpStatus httpStatus;

    @Getter(AccessLevel.PUBLIC)
    private TemplateType templateType;

    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PUBLIC)
    private ServiceScope serviceScope;

    public void init(T1 request, T2 response, TemplateType templateType) {
        this.httpStatus = HttpStatus.OK;
        this.request = request;
        this.response = new Response().withBody(response);
        this.templateType = templateType;
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
    public Integer getCode() { return response.getCode(); }

    public User getCurrentUser() {
        return User.builder().userId(getRequest().getUserId())
                .corpId(getRequest().getCorpId()).build();
    }
}
