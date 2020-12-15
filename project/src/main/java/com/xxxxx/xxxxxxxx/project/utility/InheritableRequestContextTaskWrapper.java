/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/7/1 1:21
 */

package com.xxxxx.xxxxxxxx.project.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class InheritableRequestContextTaskWrapper {
    private final static Logger logger = LoggerFactory.getLogger(InheritableRequestContextTaskWrapper.class);

    private final Map parentMDC = MDC.getCopyOfContextMap();
    private final RequestAttributes parentAttrs = RequestContextHolder.currentRequestAttributes();
    private Map<String, String> orinMDC = null;
    private RequestAttributes orinAttrs = null;

    public <R> Supplier<R> lambda0(Supplier<R> runnable) {
        return () -> {
            switchContext();
            try {
                return runnable.get();
            } finally {
                rewindContext();
            }
        };
    }

    public <T, R> Function<T, R> lambda1(Function<T, R> runnable) {
        return t -> {
            switchContext();
            try {
                return runnable.apply(t);
            } finally {
                rewindContext();
            }
        };
    }

    private void switchContext() {
        orinMDC = MDC.getCopyOfContextMap();
        if (parentMDC == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(parentMDC);
        }

        try {
            orinAttrs = RequestContextHolder.currentRequestAttributes();
        } catch (IllegalStateException e) {
            logger.debug("Worker thread without current request attributes, error message: {}",
                    e.getMessage());
        }
        RequestContextHolder.setRequestAttributes(parentAttrs, true);
    }

    private void rewindContext() {
        if (orinMDC == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(orinMDC);
        }
        if (orinAttrs == null) {
            RequestContextHolder.resetRequestAttributes();
        } else {
            RequestContextHolder.setRequestAttributes(orinAttrs, true);
        }
    }
}
