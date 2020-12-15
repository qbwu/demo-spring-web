/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/19 17:18
 */

package com.xxxxx.xxxxxxxx.project.model.exception;

public class BoundException extends RuntimeException {
    public BoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public BoundException(String message) {
        super(message);
    }
    public BoundException(Throwable cause) {
        super(cause);
    }
}
