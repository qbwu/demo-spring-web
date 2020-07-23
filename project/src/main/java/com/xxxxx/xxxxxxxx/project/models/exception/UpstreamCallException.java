/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/8 20:58
 */

package com.xxxxx.xxxxxxxx.project.models.exception;

import lombok.AccessLevel;
import lombok.Getter;

public class UpstreamCallException extends BoundException {
    @Getter(AccessLevel.PUBLIC) private String name;
    @Getter(AccessLevel.PUBLIC) private Integer code = 200;
    @Getter(AccessLevel.PUBLIC) private String detail;

    public UpstreamCallException(String message, String detail, String name,
                                 Integer code, Throwable cause) {
        super(message, cause);
        this.name = name;
        this.code = code;
        this.detail = detail;
    }

    public UpstreamCallException(String message, String detail,
                                 String name, Integer code) {
        this(message, detail, name, code, null);
    }

    public UpstreamCallException(Throwable cause, String detail,
                                 String name, Integer code) {
        this(null, detail, name, code, cause);
    }
}
