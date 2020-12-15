/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/11 17:50
 */

package com.xxxxx.xxxxxxxx.project.intercepter;

import com.xxxxx.driver4j.bns.StringUtils;
import com.xxxxx.xxxxxxxx.project.model.Protocol.StatusCode;
import com.xxxxx.xxxxxxxx.project.utility.RequestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {
    private static Logger logger = LoggerFactory.getLogger(RequestLogInterceptor.class);
    private static final String kInternalLogId = "LOGID";
    private static final String kRequestLogId = "X-LOGID";

    @Autowired
    private RequestLogger requestLogger;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        String requestLogId = getLogId(request);
        requestLogger.setLogId(requestLogId);
        requestLogger.setClientIp(getClientAddr(request));
        requestLogger.setUri(request.getRequestURI());
        requestLogger.setServerName(request.getServerName());
        requestLogger.setMethod(request.getMethod());
        requestLogger.setQueryString(request.getQueryString());

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String value = request.getHeader(headerName);
            headers.put(headerName, value);
        }
        requestLogger.setHeaders(headers);
        MDC.put(kInternalLogId, getInternalLogId(request));
        MDC.put(kRequestLogId, requestLogId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler,
                                 @Nullable Exception ex) {
        try {
            Integer code = requestLogger.getCode();
            if (code == null) {
                // code == null when some exception in dispatchController, usually it is
                // a 404 NOT_FOUND response, usually we don't care those cases so much.
                requestLogger.debug();

            } else if (code == StatusCode.InternalError.value()) {
                // Server-side severe error
                requestLogger.error();

            } else if (code >= StatusCode.UpstreamError.value()) {
                // Server-side error
                requestLogger.warn();

            } else {
                // OK or Client-side error
                requestLogger.info();
            }
        } catch (Exception e) {
            logger.error("Caught exception when do the request log", e);
        }
        MDC.clear();
    }

    public String getClientAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return ip;
    }

    String getLogId(HttpServletRequest request) {
        // According to the the document, getHeader() should be case-insensitive.
        String logId = request.getHeader("X-LOGID");
        if (StringUtils.isEmpty(logId)) {
            UUID uuid = UUID.randomUUID();
            logId = Long.toString(Math.abs(uuid.getMostSignificantBits() + uuid.getLeastSignificantBits()));
            logger.warn("Missing X-LOGID in header, generate one: {}", logId);
        }
        return logId;
    }

    String getInternalLogId(HttpServletRequest request) {
        return String.valueOf(Math.abs((System.identityHashCode(request) << 16)
                + (System.currentTimeMillis() & 65535)));
    }
}
