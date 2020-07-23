/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/12 11:25
 */

package com.xxxxx.xxxxxxxx.project.services;

import com.xxxxx.xxxxxxxx.project.models.exception.UpstreamCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.Serializable;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PMService {
    private final static Logger logger = LoggerFactory.getLogger(PMService.class);

    private final UserPermChecker userPermChecker;
    private final RequestContext context;

    private final ForkJoinPool listThreadPool;

    @Autowired
    public PMService(UserPermChecker userPermChecker, RequestContext context,
                     @Value("${service.concurrency.fork-join.limit.list: #{null}}")
                     Integer listConcurrencyLimit) {
        this.userPermChecker = userPermChecker;
        this.context = context;

        listThreadPool = listConcurrencyLimit == null ? new ForkJoinPool()
            : new ForkJoinPool(listConcurrencyLimit);
        logger.debug("listConcurrencyLimit(service.concurrency.fork-join.limit.list)={}",
                listThreadPool.getParallelism());
    }

    // Notice that @Retryable is OK because the methods are idempotent, because the
    // DB has rollback and the methods have no in-memory side-effects.
    @Retryable(value = {
            TransactionTimedOutException.class, SQLTransactionRollbackException.class
    }, maxAttempts=2, backoff=@Backoff(delay=500, maxDelay=1000))
    public void list(Boolean isTmpl) throws StaticException {
        try {
            // do something
        } catch (BoundException e) {
            handleServiceException(e);
        }
    }

    private void handleServiceException(BoundException e) {
        RequestContext context = SpringApplicationContextHolder
                        .getContext().getBean(RequestContext.class);

        if (e instanceof RequestParamException) {
            logger.debug("PMService caught RequestParamException: {}", e.getMessage(), e);
            if (context.isStatusOK()) {
                context.setStatus(HttpStatus.BAD_REQUEST,
                        StatusCode.BadRequest, e.getMessage());
            }
        } else if (e instanceof UpstreamCallException) {
            UpstreamCallException ue = (UpstreamCallException) e;
            logger.warn("PMService caught UpstreamCallException: {}, {}",
                    ue.getMessage(), ue.getDetail(), ue);
            if (context.isStatusOK()) {
                context.setStatus(HttpStatus.BAD_GATEWAY,
                        StatusCode.UpstreamError, String.format(
                                "%s, reason: called '%s', code=%d", ue.getMessage(),
                                ue.getName(), ue.getCode()));
            }
        } else {
            logger.error("PMService caught BoundException: {}", e.getMessage(), e);
            if (context.isStatusOK()) {
                context.setStatus(HttpStatus.INTERNAL_SERVER_ERROR,
                        StatusCode.InternalError);
            }
        }
    }
}
