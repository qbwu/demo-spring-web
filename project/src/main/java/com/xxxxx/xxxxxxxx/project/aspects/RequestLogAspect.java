/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/11 13:19
 */

package com.xxxxx.xxxxxxxx.project.aspects;

import com.xxxxx.xxxxxxxx.project.models.RequestContext;
import com.xxxxx.xxxxxxxx.project.utility.RequestLogger;
import com.xxxxx.xxxxxxxx.project.utility.RequestLogger.CallProfiler;
import com.xxxxx.xxxxxxxx.project.utility.SpringApplicationContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;

@Aspect
@Component
public class RequestLogAspect {
    private static Logger logger = LoggerFactory.getLogger(RequestLogAspect.class);
    @Autowired
    RequestLogger requestLogger;

    @Around("execution(* com.xxxxx.xxxxxxxx.project.library.HttpClientUtil.httpPostJson(..))")
    public Object passLogId(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        Map<String, String> headers = (Map<String, String>) args[args.length - 1];
        headers.put("X-LOGID", requestLogger.getLogId());

        long startTime = System.currentTimeMillis();
        Object res = joinPoint.proceed();
        long endTime = System.currentTimeMillis();

        URL url = new URL((String) args[0]);
        // TODO record the retry times in the future
        requestLogger.addCallProfiler(CallProfiler.builder()
                .host(url.getHost()).uri(url.getPath())
                .timeCost(endTime - startTime).build());
        return res;
    }

    @Before("execution(public * com.xxxxx.xxxxxxxx.project.services.PMService.*(..))")
    public void beforeGetInService(JoinPoint joinPoint) {
        RequestContext reqCtx = SpringApplicationContextHolder.getContext()
                .getBean(RequestContext.class);
        requestLogger.setRequestBody(reqCtx.getRequest());
        requestLogger.setResponseBody(reqCtx.buildResponseEntity().getBody());
    }

    @AfterReturning(value = "execution(public org.springframework.http.ResponseEntity " +
            "com.xxxxx.xxxxxxxx.project.controllers.PMController.controllerFailedResponse(..))",
            returning = "response")
    public void afterFailedResponse(JoinPoint joinPoint, Object response) {
        requestLogger.setResponseBody(((ResponseEntity) response).getBody());
    }

    @Aspect
    @Component
    @ConditionalOnExpression("${mapper.profiler.enabled:false}")
    public class OptionalMapperProfiler {
        @Around("execution(* com.xxxxx.xxxxxxxx..mappers.*.*(..))")
        public Object projectMapperProfiler(ProceedingJoinPoint joinPoint) throws Throwable {
            long startTime = System.currentTimeMillis();
            logger.debug("\n========= MAPPER {} BEGIN =========", joinPoint.getSignature().getName());
            Object res = joinPoint.proceed();
            logger.debug("\n========= MAPPER {} END =========", joinPoint.getSignature().getName());
            long timeCost = System.currentTimeMillis() - startTime;
            String methodKey = String.format("%s.%s",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
            requestLogger.accumulate(String.format("%s.timeMs", methodKey), timeCost);
            requestLogger.accumulate(String.format("%s.times", methodKey), 1L);
            requestLogger.accumulate("mysqlTimeMs", timeCost);
            return res;
        }
    }
}
