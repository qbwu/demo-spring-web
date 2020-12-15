/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/10 21:54
 */

package com.xxxxx.xxxxxxxx.project.utility;

import com.xxxxx.xxxxxxxx.project.library.hi.AuthInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class RequestLogger {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Value("${request-logger.min-verbose}")
    @Setter(AccessLevel.PUBLIC)
    private Integer minVerbose = 2;

    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PUBLIC)
    private String logId;

    @Setter(AccessLevel.PUBLIC)
    private String uri;

    @Setter(AccessLevel.PUBLIC)
    private String clientIp;

    @Setter(AccessLevel.PUBLIC)
    private String serverName;

    @Setter(AccessLevel.PUBLIC)
    private String method;

    @Setter(AccessLevel.PUBLIC)
    private Object requestBody;

    @Setter(AccessLevel.PUBLIC)
    private Object responseBody;

    @Setter(AccessLevel.PUBLIC)
    private Map<String, String> headers;

    @Setter(AccessLevel.PUBLIC)
    private String queryString;

    private Integer code;

    @Setter(AccessLevel.PUBLIC)
    private AuthInfo authInfo;

    private final long startTime = System.currentTimeMillis();
    private final Queue<CallProfiler> callProfilers = new ConcurrentLinkedQueue<>();

    private final Map<String, Long> accumulators = new ConcurrentHashMap<>();

    @Data
    @Builder
    public static class CallProfiler {
        private String host;
        private String uri;
        private Integer code;
        private long retry = -1;
        private long timeCost = -1;
    }

    // Could be called from aspects in multithreads
    public void addCallProfiler(CallProfiler callProfiler) {
        callProfilers.add(callProfiler);
    }

    // Could be called from aspects in multithreads
    public void accumulate(String name, Long delta) {
        accumulators.merge(name, delta, Long::sum);
    }

    public void debug() {
        logger.debug("{}{}", logHeader(), logBody(1));
    }

    public void info() {
        logger.info("{}{}", logHeader(), logBody(1));
    }

    public void warn() {
        logger.warn("{}{}", logHeader(), logBody(3));
    }

    public void error() {
        logger.error("{}{}", logHeader(), logBody(3));
    }

    private String logHeader() {
        long endTime = System.currentTimeMillis();

        String accum;
        String profilers;

        try {
            accum = jsonMapper.writeValueAsString(accumulators);
        } catch (Exception e) {
            accum = String.format("Failed to serialize accumulators, reason: %s", e.getMessage());
        }

        try {
            profilers = jsonMapper.writeValueAsString(callProfilers);
        } catch (Exception e) {
            profilers = String.format("Failed to serialize callProfilers, reason: %s", e.getMessage());
        }

        return String.format(
                ("[logId:%s][method:%s][uri:%s][code:%d][totalTime:%dms]" +
                        "[serverName:%s][clientIp:%s][accums:%s][stages:%s]"),
                logId, method, uri, getCode(), endTime - startTime, serverName, clientIp,
                accum, profilers);
    }
    // Only be called after finishing request process
    public Integer getCode() {
        if (code != null) {
            return code;
        }
        try {
            code = responseBody == null ? null :
                    (Integer) responseBody.getClass().getMethod("getCode")
                            .invoke(responseBody);
        } catch (Exception e) {
            logger.info("Failed to get code from response", e);
        }
        return code;
    }

    private String logBody(int verbose) {
        try {
            int vrbs = verbose > this.minVerbose ? verbose : this.minVerbose;
            switch (vrbs) {
                case 0:
                    return "";
                case 1:
                    return String.format("{ identity:=%s }{ param:={%s} }{ req:=%s }",
                            jsonMapper.writeValueAsString(authInfo),
                            queryString,
                            jsonMapper.writeValueAsString(requestBody));
                case 2:
                    return String.format("{ identity:=%s }{ param:={%s} }{ req:=%s }{ resp:=%s }",
                            jsonMapper.writeValueAsString(authInfo),
                            queryString,
                            jsonMapper.writeValueAsString(requestBody),
                            jsonMapper.writeValueAsString(responseBody));
                default: // verbose >= 3
                    return String.format("{ identity:=%s }{ param:={%s} }{ req:=%s }{ resp:=%s }{ header:=%s }",
                            jsonMapper.writeValueAsString(authInfo),
                            queryString,
                            jsonMapper.writeValueAsString(requestBody),
                            jsonMapper.writeValueAsString(responseBody),
                            jsonMapper.writeValueAsString(headers));
            }
        } catch (Exception e) {
            return String.format("Failed to serialize request or response, reason: %s",
                    e.getMessage());
        }
    }
}
