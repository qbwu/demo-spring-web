/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/11 13:19
 */

package com.xxxxx.xxxxxxxx.project.aspect;

import com.xxxxx.xxxxxxxx.project.model.RequestContext;
import com.xxxxx.xxxxxxxx.project.utility.RequestLogger;
import com.xxxxx.xxxxxxxx.project.utility.RequestLogger.CallProfiler;
import com.xxxxx.xxxxxxxx.project.utility.framework.SpringApplicationContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class RequestLogAspect {
    private static Logger logger = LoggerFactory.getLogger(RequestLogAspect.class);

    // This component is a low-level facility, it could be used both in request context
    // and offline context, we must take care for the both cases.
    @Autowired
    RequestLogger requestLogger;

    @Around("execution(* com.xxxxx.xxxxxxxx.project.utility.HttpClientUtils.http*(..))")
    public Object passLogId(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        Map<String, String> headers = (Map<String, String>) args[args.length - 1];
        if (headers == null) {
            headers = new HashMap<>();
        }

        boolean inRequestScope = RequestContextHolder.getRequestAttributes() != null;
        if (inRequestScope) {
            headers.put("X-LOGID", requestLogger.getLogId());

        } else if (MDC.get("LOGID") != null) {
            headers.put("X-LOGID", MDC.get("LOGID"));
        }

        args[args.length - 1] = headers;

        long startTime = System.currentTimeMillis();
        Object res = joinPoint.proceed();
        long endTime = System.currentTimeMillis();

        URL url = new URL((String) args[0]);
        // TODO record the retry times in the future
        if (inRequestScope) {
            requestLogger.addCallProfiler(CallProfiler.builder()
                    .host(url.getHost()).uri(url.getPath())
                    .timeCost(endTime - startTime).build());
        }
        return res;
    }

    @Before("execution(public * com.xxxxx.xxxxxxxx.project.service.PMService.*(..))")
    public void beforeGetInService(JoinPoint joinPoint) {
        RequestContext reqCtx = SpringApplicationContextHolder.getContext()
                .getBean(RequestContext.class);
        requestLogger.setRequestBody(reqCtx.getRequest());
        requestLogger.setResponseBody(reqCtx.buildResponseEntity().getBody());
    }

    @AfterReturning(value = "execution(public org.springframework.http.ResponseEntity " +
            "com.xxxxx.xxxxxxxx.project.controller.PMController.controllerFailedResponse(..))",
            returning = "response")
    public void afterFailedResponse(JoinPoint joinPoint, Object response) {
        requestLogger.setResponseBody(((ResponseEntity) response).getBody());
    }

    @AfterReturning(value =
            "execution(* com.xxxxx.xxxxxxxx.file.dto.BaseResponseDTO.*(..))",
            returning = "returnedVal")
    public void afterFileResponse(JoinPoint joinPoint, Object returnedVal) {
        requestLogger.setResponseBody(returnedVal);
    }

    @Aspect
    @Component
    @ConditionalOnExpression("${profiler.aspect.enabled:false}")
    public class OptionalProfilerAspect {

        @Around("execution(* com.xxxxx.xxxxxxxx..mapper.*.*(..))")
        public Object mapperProfiler(ProceedingJoinPoint joinPoint) throws Throwable {
            long startTime = System.currentTimeMillis();
            logger.debug("\n========= Mapper {} BEGIN =========", joinPoint.getSignature().getName());

            Object res = joinPoint.proceed();

            long timeCost = System.currentTimeMillis() - startTime;
            logger.debug("\n========= Mapper {} END ({}ms) =========",
                    joinPoint.getSignature().getName(), timeCost);
            if (RequestContextHolder.getRequestAttributes() != null) {
                String joinPointKey = getJoinPointKey(joinPoint);
                requestLogger.accumulate(String.format("%s.timeMs", joinPointKey), timeCost);
                requestLogger.accumulate(String.format("%s.times", joinPointKey), 1L);
                requestLogger.accumulate("mysqlTimeMs", timeCost);
            }
            return res;
        }

        @Around("execution(* com.xxxxx.xxxxxxxx.project.cache.serializer.KryoRedisSerializer.*serialize(..))")
        public Object redisSerializerProfiler(ProceedingJoinPoint joinPoint) throws Throwable {
            long startTime = System.currentTimeMillis();
            logger.debug("\n========= Serializer {} BEGIN =========", joinPoint.getSignature().getName());

            Object res = joinPoint.proceed();

            long timeCost = System.currentTimeMillis() - startTime;
            logger.debug("\n========= Serializer {} END ({}ms) =========",
                    joinPoint.getSignature().getName(), timeCost);

            if (RequestContextHolder.getRequestAttributes() != null) {
                String joinPointKey = getJoinPointKey(joinPoint);
                requestLogger.accumulate(String.format("%s.timeMs", joinPointKey), timeCost);
                requestLogger.accumulate(String.format("%s.times", joinPointKey), 1L);
                requestLogger.accumulate("serializationTimeMs", timeCost);

                String methodName = joinPoint.getSignature().getName();
                byte[] bytes = (byte[]) (methodName.startsWith("ser") ? res : joinPoint.getArgs()[0]);
                long bytesLen = res == null ? 0 : bytes.length;
                requestLogger.accumulate(String.format("%s.bytes", joinPointKey), bytesLen);
            }
            return res;
        }

        private String getJoinPointKey(JoinPoint joinPoint) {
            return String.format("%s.%s",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
        }
    }
}
