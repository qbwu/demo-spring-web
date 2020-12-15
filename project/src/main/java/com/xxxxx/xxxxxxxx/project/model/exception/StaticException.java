/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/20 1:07
 */

package com.xxxxx.xxxxxxxx.project.model.exception;

public class StaticException extends Exception {
    public StaticException(String message, Throwable cause) {
        super(message, cause);
    }
    public StaticException(String message) {
        super(message);
    }
    public StaticException(Throwable cause) {
        super(cause);
    }
}
