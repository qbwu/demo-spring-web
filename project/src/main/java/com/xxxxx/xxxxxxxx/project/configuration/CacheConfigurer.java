/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/29 14:28
 */

package com.xxxxx.xxxxxxxx.project.configuration;

import com.xxxxx.xxxxxxxx.project.cache.RecoverableCacheErrorHandler;
import com.xxxxx.xxxxxxxx.project.model.template.ExtValueTemplateAttr;
import com.xxxxx.xxxxxxxx.project.model.template.Templatable;
import com.xxxxx.xxxxxxxx.project.model.template.TypedExtTemplateAttr;
import com.xxxxx.xxxxxxxx.project.model.template.TypedSingletonExtTemplateAttr;
import com.xxxxx.xxxxxxxx.project.cache.serializer.AopProxySerializer;
import com.xxxxx.xxxxxxxx.project.cache.serializer.AutowireSerializer;
import com.xxxxx.xxxxxxxx.project.cache.serializer.KryoRedisSerializer;
import com.xxxxx.xxxxxxxx.templateengine.models.Thing;
import com.xxxxx.xxxxxxxx.templateengine.models.ThingAttr;
import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.event.metrics.CommandLatencyEvent;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.resource.DefaultClientResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xxxxx.xxxxxxxx.project.cache.RequestScopedCacheManager;
import com.xxxxx.xxxxxxxx.project.model.data.TemplatableVisitor;
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
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class CacheConfigurer {
    public static final String kCacheManagerNameRequestScoped = "CacheManager-request-scoped";
    public static final String kCacheManagerNameLocalized = "CacheManager-localized";
    public static final String kCacheManagerNameRemote = "CacheManager-remote";

    public static final String kCacheNameUserPortrait = "Cache-userPortrait";
    public static final String kCacheNameVisitTime = "Cache-visitTime";
    public static final String kCacheNameAttrs = "Cache-attrs";

    public static final String kCacheEventListenerNameVisitTime = "CacheEventListener-visitTime";

    private static Logger logger = LoggerFactory.getLogger(CacheConfigurer.class);

    @Value("${service.cache.visitTime.heap.usage.kb}")
    private Long cacheVisitTimeHeapUsageKB;
    @Value("${service.cache.visitTime.expiry.sec}")
    private Long cacheVisitTimeExpirySec;

    @Value("${service.cache.attrs.expiry.sec}")
    private Long cacheAttrsExpirySec;

    @Value("${service.cache.attrs.compression}")
    private Boolean cacheAttrsCompression;

    @Value("${redis.command.latency.metrics.interval.sec}")
    private Long LatencyMetricsIntervalSec;

    @Value("${redis.command.latency.metrics.enable}")
    private Boolean enableLatencyMetrics;

    @Autowired
    @Qualifier(kCacheEventListenerNameVisitTime)
    private CacheEventListener visitTimeCacheEventListener;

    @Autowired
    CacheConfigurer(CacheInterceptor cacheInterceptor) {
        cacheInterceptor.configure(RecoverableCacheErrorHandler::new,
                null, null, null);
    }

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

    @Bean(kCacheManagerNameRemote)
    public CacheManager remoteCacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
        Map<String, RedisCacheConfiguration>
                cacheConfigMap = new HashMap<String, RedisCacheConfiguration>() {
            { put(kCacheNameAttrs, attrCacheConfig()); }
        };

        if (enableLatencyMetrics) {
            lettuceConnectionFactory.getClientResources().eventBus().get()
                    .filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                    .subscribe(e -> logger.info("Redis command latency: {}", e));
        }

        return RedisCacheManager.builder(lettuceConnectionFactory)
                .initialCacheNames(Stream.of(kCacheNameAttrs).collect(Collectors.toSet()))
                .withInitialCacheConfigurations(cacheConfigMap)
                .build();
    }

    @Bean
    public KryoRedisSerializer<Templatable> kryoSerializerOfTemplatable() {
        KryoRedisSerializer<Templatable> ser = new KryoRedisSerializer<>(
                Templatable.class, cacheAttrsCompression);

        ser.addDefaultSerializer(Templatable.class, AopProxySerializer.class);
        ser.addDefaultSerializer(Thing.class, AutowireSerializer.class);
        ser.addDefaultSerializer(ExtValueTemplateAttr.class, AutowireSerializer.class);

        // The order of appearance of the classes should not change
        ser.register(Templatable.class, ThingAttr.class, ExtValueTemplateAttr.class,
                TypedExtTemplateAttr.class, TypedSingletonExtTemplateAttr.class);
        return ser;
    }

    @Bean(destroyMethod = "shutdown")
    public DefaultClientResources lettuceClientResources() {
        DefaultClientResources.Builder builder = DefaultClientResources.builder();
        if (enableLatencyMetrics) {
            builder.commandLatencyCollectorOptions(
                    DefaultCommandLatencyCollectorOptions.builder()
                           // timeunit of the latencies
                           .targetUnit(TimeUnit.MILLISECONDS).build())
                   .commandLatencyPublisherOptions(
                        DefaultEventPublisherOptions.builder()
                            // publish the latency statistics every a number of seconds
                            .eventEmitInterval(Duration.ofSeconds(LatencyMetricsIntervalSec)).build());
        }
        return builder.build();
    }

    private javax.cache.CacheManager ehCacheManager() {
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

    private RedisCacheConfiguration attrCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // Notice that spring does not support setting maxSize for Redis cache, so we have to
                // use the ttl to limit the size of redis
                .entryTtl(Duration.ofSeconds(cacheAttrsExpirySec))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        kryoSerializerOfTemplatable()))
                // not caching null values
                .disableCachingNullValues();
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
