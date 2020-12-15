/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/12 11:25
 */

package com.xxxxx.xxxxxxxx.project.service;

import com.xxxxx.xxxxxxxx.project.manager.ExternalLogPublisher;
import com.xxxxx.xxxxxxxx.project.manager.UserPermManager;
import com.xxxxx.xxxxxxxx.project.mapper.AssociationMapper;
import com.xxxxx.xxxxxxxx.project.model.enums.ServiceScope;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.exception.UpstreamCallException;
import com.xxxxx.xxxxxxxx.project.utility.IdAttrComparator;
import com.xxxxx.xxxxxxxx.project.utility.InheritableRequestContextTaskWrapper;
import com.xxxxx.xxxxxxxx.project.model.exception.StaticException;
import com.xxxxx.xxxxxxxx.project.utility.framework.SpringApplicationContextHolder;
import com.xxxxx.xxxxxxxx.project.model.Protocol.StatusCode;
import com.xxxxx.xxxxxxxx.project.model.Protocol.Attr;
import com.xxxxx.xxxxxxxx.project.model.Protocol.IdAttr;
import com.xxxxx.xxxxxxxx.project.model.Protocol.ListRequest;
import com.xxxxx.xxxxxxxx.project.model.Protocol.ListResponse;
import com.xxxxx.xxxxxxxx.project.model.Protocol.GetAttrRequest;
import com.xxxxx.xxxxxxxx.project.model.Protocol.GetAttrResponse;
import com.xxxxx.xxxxxxxx.project.model.Protocol.SetAttrRequest;
import com.xxxxx.xxxxxxxx.project.model.Protocol.SetAttrResponse;
import com.xxxxx.xxxxxxxx.project.model.Protocol.SetAttrValueRequest;
import com.xxxxx.xxxxxxxx.project.model.Protocol.SetAttrValueResponse;
import com.xxxxx.xxxxxxxx.project.model.RequestContext;
import com.xxxxx.xxxxxxxx.project.model.exception.BoundException;
import com.xxxxx.xxxxxxxx.project.model.exception.RequestParamException;
import com.xxxxx.xxxxxxxx.project.model.template.AttrConfig;
import com.xxxxx.xxxxxxxx.project.model.template.ExtValueTemplateAttr;
import com.xxxxx.xxxxxxxx.project.model.template.Templatable;
import com.xxxxx.xxxxxxxx.templateengine.models.TemplateAttr;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.PoolException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.util.StringUtils;
import java.sql.SQLTransactionRollbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.Serializable;
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

    private final RequestContext context;

    private final UserPermManager userPermChecker;
    private final AssociationMapper associationMapper;

    private final Boolean attrAdaptive;
    private final Boolean enableKgFlowLogsink;

    private final ForkJoinPool listThreadPool;

    private final ExternalLogPublisher externalLogPublisher;

    @Resource
    private PMService self;

    @Autowired
    public PMService(RequestContext context,
                     UserPermManager userPermChecker,
                     AssociationMapper associationMapper,
                     ExternalLogPublisher externalLogPublisher,
                     @Value("${service.concurrency.fork-join.limit.list: #{null}}") Integer listConcurrencyLimit,
                     @Value("${service.function.attr.adaptive: #{false}}") Boolean attrAdaptive,
                     @Value("${service.logsink.kgflow.enable: #{false}}") Boolean enableKgFlowLogsink) {

        this.userPermChecker = userPermChecker;
        this.context = context;
        this.associationMapper = associationMapper;
        this.attrAdaptive = attrAdaptive;
        this.externalLogPublisher = externalLogPublisher;
        this.enableKgFlowLogsink = enableKgFlowLogsink;
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
            ListRequest req = (ListRequest) context.getRequest();
            ListResponse resp = (ListResponse) context.getResponse();

            Templatable templatable =
                    SpringApplicationContextHolder.getContext().getBean(Templatable.class);

            Set<String> ids = templatable.filter(limitIds, attrValues);

            List<IdAttr> attrList;
            try {
                InheritableRequestContextTaskWrapper wrapper = new InheritableRequestContextTaskWrapper();
                attrList = listThreadPool.submit(() -> ids.parallelStream().map(
                        wrapper.lambda1((String id) -> {
                            try {
                                Templatable temp = getTemplatableForRead(id);
                                temp.setDynamic(attrAdaptive);

                                return new IdAttr(temp.getId(), temp.getAttrValues(req.getReturnAttrs()));
                            } catch (Exception e) {
                                logger.error("Error occurred in async tasks, stack trace: ", e);
                                throw new BoundException("Error occurred in async tasks", e);
                            }
                        }
                )).sorted(new IdAttrComparator(req.getSort())).collect(Collectors.toList())).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BoundException("Error occurred in parallel threads", e);
            }

            resp.setList(attrList);

        } catch (BoundException e) {
            handleServiceException(e);
        }
    }


    @Retryable(value = {
            TransactionTimedOutException.class, SQLTransactionRollbackException.class
    }, maxAttempts=2, backoff=@Backoff(delay=500, maxDelay=1000))
    public void setValue(HttpMethod method) throws StaticException {
        try {
            SetAttrValueRequest req = (SetAttrValueRequest) context.getRequest();
            SetAttrValueResponse resp = (SetAttrValueResponse) context.getResponse();
            TemplateType tmplType = context.getTemplateType();

            List<ExtValueTemplateAttr> extAttrs = new ArrayList<>();
            Map<String, Object> attrValues = new HashMap<>();


            Templatable templatable = getOrDeriveTemplatableForUpdate(
                    req.getId(), req.getOrinId(), tmplType, attrNames);
            templatable.setDynamic(attrAdaptive);

            templatable.setAttrs(extAttrs);


            // Must evict the cache after updating values in the DB, to minimize the
            // possibility of holding dirty data in the cache (In case of one reads and
            // loads the data in the cache, before another one writing and updating the
            // data in the DB but after evicting the data in the cache).
            templatable.save();
            self.evictCachedTemplatable(templatable.getId());

            // Generally, we do not change the in-memory states of attributes in the saving
            // stage (that is the method saveExtValue). But there still are a few cases where
            // we have to defer some modifications until saving, as an example, if users want
            // to update two dependent attributes in the same request.
            Map<String, Attr> attrs = buildAttrMap(templatable.getAttrs(
                    new ArrayList<>(req.getReturnAttrs())));

            resp.setId(templatable.getId());
            resp.setAttrs(attrs);

            if (context.getServiceScope().equals(ServiceScope.CLIENT)) {
                updateProjectVisitTime(templatable.getId(), kAttrNameSysProjectUpdateTime);
            }
        } catch (BoundException e) {
            handleServiceException(e);
        }
    }

    private Templatable getOrDeriveTemplatableForUpdate(String id, String orinId,
            TemplateType tmplType, Set<String> attrNames) throws StaticException {


        Templatable templatable =
                SpringApplicationContextHolder.getContext().getBean(Templatable.class);
        return templatable;
    }

    private Templatable getTemplatableForRead(String id) throws StaticException {
        try {
            return self.getCachedTemplatable(id);
        } catch (DataAccessException | PoolException ex) {
            logger.warn("Failed to connect to redis to get templatable: ", ex);
        }
        // fallback path
        Templatable ret = SpringApplicationContextHolder.getContext()
                .getBean(Templatable.class);
        ret.setId(id);
        return ret;
    }

    @Cacheable(cacheManager = kCacheManagerNameRemote, cacheNames = {kCacheNameAttrs})
    public Templatable getCachedTemplatable(String id) throws StaticException {
        Templatable temp = SpringApplicationContextHolder.getContext().getBean(Templatable.class);
        temp.setId(id);
        temp.load();
        // We don't cache the sysProjectViewTime, it is too volatile.
        temp.evictAttrs(Stream.of(kAttrNameSysProjectViewTime).collect(Collectors.toSet()));
        if (enableKgFlowLogsink) {
            externalLogPublisher.postChangeLog(ExternalLogPublisher.Operation.Update, temp);
        }
        return temp;
    }

    @CacheEvict(cacheManager = kCacheManagerNameRemote, cacheNames = {kCacheNameAttrs})
    public void evictCachedTemplatable(String id) {

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
