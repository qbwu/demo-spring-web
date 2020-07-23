/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/29 14:28
 */

package com.xxxxx.xxxxxxxx.project.configurations;

import com.xxxxx.xxxxxxxx.project.caches.RequestScopedCacheManager;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfigurer {
    public static final String kCacheNameSomethingInRequest = "SomethingInRequest";
    public static final String kCacheNameSomeThingInEhCache = "SomethingInEhCache";
    @Bean
    public CacheManager requestCacheManager() {
        return new RequestScopedCacheManager(kCacheNameUserPortraitInRequest);
    }
    
    @Bean
    public CacheManager localizedCacheManager() {
        return new JCacheCacheManager(ehCacheManager());
    }

    @Bean(destroyMethod = "close")
    public javax.cache.CacheManager ehCacheManager() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        EhcacheCachingProvider ehcacheCachingProvider = (EhcacheCachingProvider) cachingProvider;

        DefaultConfiguration defaultConfiguration = new DefaultConfiguration(
                new HashMap<String, CacheConfiguration<?, ?>>() {
                    {
                        put(kCacheNameSomeThingInEhCache, someThingInEhCacheCacheConfig());
                    }
                }, ehcacheCachingProvider.getDefaultClassLoader()
        );
        return ehcacheCachingProvider.getCacheManager(
                ehcacheCachingProvider.getDefaultURI(), defaultConfiguration);
    }

    private CacheConfiguration<String, Long> someThingInEhCacheCacheConfig() {
        // TODO make it configurable
        return CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Long.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, MemoryUnit.KB))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5)))
                .build();
    }
}
