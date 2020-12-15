/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/7/30 16:15
 */

package com.xxxxx.xxxxxxxx.project.model.template;

import com.xxxxx.xxxxxxxx.project.mapper.TemplatableVisitTimeMapper;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.data.DateRange;
import com.xxxxx.xxxxxxxx.project.model.data.TemplatableVisitor;
import org.ehcache.impl.events.CacheEventAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static com.xxxxx.xxxxxxxx.project.configuration.CacheConfigurer.kCacheEventListenerNameVisitTime;
import static com.xxxxx.xxxxxxxx.project.configuration.CacheConfigurer.kCacheManagerNameLocalized;
import static com.xxxxx.xxxxxxxx.project.configuration.CacheConfigurer.kCacheNameVisitTime;

public class TemplatableViewTimeAttr extends TemplatableVisitTimeAttr {

    private static final long serialVersionUID = 1L;

    @Autowired
    private TemplatableVisitTimeMapper dao;

    @Autowired
    private CacheHelper cacheHelper;

    public TemplatableViewTimeAttr(TemplateType objectType) {
        super(objectType, TemplatableVisitor.VisitType.VIEW);
    }

    private TemplatableViewTimeAttr() {

    }

    @Override
    protected final Long getLatestTime(TemplatableVisitor visitor) {
        // The cache may hold the latest timestamps which have not been flushed
        // to the database so we have to adjust the value loaded by template engine.
        Long latest = cacheHelper.getFromCache(visitor);
        if (latest != null) {
            return latest;
        } else {
            latest = dao.getVisitTime(visitor);
            if (latest != null) {
                // We cannot update the cache here, because the expired data may
                // have not been synchronized to the DB asynchronously, so the
                // fetched data may be staled. And if we update the cache with
                // this staled data, after later being expired, it will override
                // the data of newer version in the DB.
                return latest;
            }
        }
        return null;
    }

    @Override
    protected final void updateLatestTime(TemplatableVisitor visitor, Long time) {
        Long latest = cacheHelper.getFromCache(visitor);
        if (latest != null && time <= latest) {
            return;
        }
        // TODO the cache updating should have transaction-awareness,
        // otherwise some needless updates will apply to the cache.
        // However we do accept the imprecise visit times, so we
        // think it is harmless to ignore the transaction by now...
        cacheHelper.updateCache(visitor, time);
    }

    @Override
    protected final Set<String> findObjectsVisitedInTime(
            TemplatableVisitor visitor, DateRange period, Set<String> limit) {
        return new HashSet<>(dao.findObjectsByVisitTime(visitor, period, limit));
    }

    // Notice that the @Component and the public access privilege is necessary for the
    // spring cache aop proxy, otherwise the methods will be invoked without any cache
    // actions.
    @Component
    @CacheConfig(cacheManager = kCacheManagerNameLocalized,
                 cacheNames = {kCacheNameVisitTime})
    public static class CacheHelper {
        private static final Logger logger = LoggerFactory.getLogger(CacheHelper.class);

        @Cacheable(key = "#visitor", unless = "#result == null")
        public Long getFromCache(TemplatableVisitor visitor) {
            logger.debug("Cache missed: {}", visitor.toString());
            return null;
        }

        @CachePut(key = "#visitor")
        public Long updateCache(TemplatableVisitor visitor, Long curr) {
            logger.debug("Cache updated: {}->{}", visitor.toString(), curr);
            return curr;
        }
    }

    // The event callbacks are processed in a lazy way, effectively at the next
    // access the cache entry.
    @Component(kCacheEventListenerNameVisitTime)
    public static final class CacheEventListener
            extends CacheEventAdapter<TemplatableVisitor, Long> {
        @Autowired
        private TemplatableVisitTimeMapper dao;

        // We can enable the mybatis log to inspect the putVisitTime being triggered on time.
        @Override
        protected void onExpiry(TemplatableVisitor visitor, Long timestamp) {
            dao.putVisitTime(visitor, timestamp);
        }
        @Override
        protected void onEviction(TemplatableVisitor visitor, Long timestamp) {
            dao.putVisitTime(visitor, timestamp);
        }
        @Override
        protected void onRemoval(TemplatableVisitor visitor, Long timestamp) {
            dao.putVisitTime(visitor, timestamp);
        }
    }
}
