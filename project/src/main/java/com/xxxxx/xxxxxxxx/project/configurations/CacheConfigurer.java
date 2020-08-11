/*
 * Copyright (c) 2020 qbwu Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/29 14:28
 */

package com.xxxxx.xxxxxxxx.project.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xxxxx.xxxxxxxx.project.caches.RequestScopedCacheManager;
import com.xxxxx.xxxxxxxx.project.models.data.TemplatableVisitor;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;

@Configuration
public class CacheConfigurer {
    public static final String kCacheManagerNameRequestScoped = "CacheManager-request-scoped";
    public static final String kCacheManagerNameLocalized = "CacheManager-localized";

    public static final String kCacheNameUserPortrait = "Cache-userPortrait";
    public static final String kCacheNameVisitTime = "Cache-visitTime";

    public static final String kCacheEventListenerNameVisitTime = "CacheEventListener-visitTime";

    private static Logger logger = LoggerFactory.getLogger(CacheConfigurer.class);

    @Value("${service.cache.visitTime.heap.usage.kb}")
    private Long cacheVisitTimeHeapUsageKB;
    @Value("${service.cache.visitTime.expiry.sec}")
    private Long cacheVisitTimeExpirySec;

    @Autowired
    @Qualifier(kCacheEventListenerNameVisitTime)
    private CacheEventListener visitTimeCacheEventListener;

    @Primary
    @Bean(kCacheManagerNameRequestScoped)
    public CacheManager requestScopedCacheManager() {
        return new RequestScopedCacheManager(kCacheNameUserPortrait);
    }

    @Bean(kCacheManagerNameLocalized)
    public CacheManager localizedCacheManager() {
        return new JCacheCacheManager(ehCacheManager()) {
            @PreDestroy
            void close() {
                logger.info("CacheManager({}) closed gracefully", kCacheManagerNameLocalized);
                Objects.requireNonNull(getCacheManager()).close();
            }
        };
    }

    public javax.cache.CacheManager ehCacheManager() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        EhcacheCachingProvider ehcacheCachingProvider = (EhcacheCachingProvider) cachingProvider;

        DefaultConfiguration defaultConfiguration = new DefaultConfiguration(
                new HashMap<String, CacheConfiguration<?, ?>>() {
                    {
                        put(kCacheNameVisitTime, visitTimeCacheConfig());
                    }
                }, ehcacheCachingProvider.getDefaultClassLoader()
        );
        javax.cache.CacheManager cacheManager = ehcacheCachingProvider.getCacheManager(
                ehcacheCachingProvider.getDefaultURI(), defaultConfiguration);
        // For performance monitor
        cacheManager.enableManagement(kCacheNameVisitTime, true);
        cacheManager.enableStatistics(kCacheNameVisitTime, true);
        return cacheManager;
    }

    private CacheConfiguration<TemplatableVisitor, Long> visitTimeCacheConfig() {
        CacheEventListenerConfigurationBuilder configuration = CacheEventListenerConfigurationBuilder
                        .newEventListenerConfiguration(visitTimeCacheEventListener,
                                EventType.EXPIRED, EventType.REMOVED, EventType.EVICTED)
                            .unordered().asynchronous();

        return CacheConfigurationBuilder.newCacheConfigurationBuilder(TemplatableVisitor.class, Long.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                            // TODO test using off-heap as the storage
                            .heap(cacheVisitTimeHeapUsageKB, MemoryUnit.KB))
                .withService(configuration)
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(
                        Duration.ofSeconds(cacheVisitTimeExpirySec)))
                .build();
    }

    @Scheduled(cron = "#{'*/' + ${service.cache.visitTime.eviction.sec} + ' * * * * *'}")
    private void visitTimeCacheCleaner() {
        long startTime = System.currentTimeMillis();
        Cache<TemplatableVisitor, Long> cache = (Cache<TemplatableVisitor, Long>)
                Objects.requireNonNull(localizedCacheManager().getCache(kCacheNameVisitTime))
                .getNativeCache();
        // Travel through the cache to trigger the eviction on expired keys, and the onEviction
        // of cache event listener will flush them to DB
        long num = 0;
        for (Cache.Entry<TemplatableVisitor, Long> entry : cache) {
            ++num;
        }
        long timeCost = System.currentTimeMillis() - startTime;
        logger.debug("Finished cleaning the visitTime cache, cache size: {}, time cost: {}ms",
                num, timeCost);
    }
}
