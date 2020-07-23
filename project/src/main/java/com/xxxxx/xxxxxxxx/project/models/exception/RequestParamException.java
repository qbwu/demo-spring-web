/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/19 17:02
 */

package com.xxxxx.xxxxxxxx.project.models.exception;

public class RequestParamException extends BoundException {
    static final String fuzzyMsg = "Wrong parameters in the request.";

    public RequestParamException() { this(fuzzyMsg); }
    public RequestParamException(String message) { this(message, null); }
    public RequestParamException(Throwable cause) { this(fuzzyMsg, cause); }
    public RequestParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
