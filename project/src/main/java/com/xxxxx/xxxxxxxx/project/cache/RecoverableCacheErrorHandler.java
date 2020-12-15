/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/10/23 20:15
 */

package com.xxxxx.xxxxxxxx.project.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.PoolException;
import org.springframework.lang.Nullable;

public class RecoverableCacheErrorHandler implements CacheErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(RecoverableCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception,
                                    org.springframework.cache.Cache cache, Object key) {
        // We need to recognize the connection exception and go through the fallback path outside.
        throw exception;
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache,
                                    Object key, @Nullable Object value) {
        // For the Redis cache, we only have a @Cacheable but no @CachePut method.
        // It's OK to give up the cache put operations for cache misses when
        // encountering connection errors.
        if (exception instanceof DataAccessException || exception instanceof PoolException) {
            logger.warn("Failed to connect to redis to do PUT: ", exception);
            return;
        }
        throw exception;
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        // For the Redis cache, although the @CacheEvict method is called after
        // the DB updated, dirty data would remain in the Cache if we just ignored
        // connection exceptions here.
        throw exception;
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        ignoreConnectionFailure(exception);
    }

    private void ignoreConnectionFailure(RuntimeException exception) {
        if (exception instanceof PoolException) {
            logger.warn("Failed to connect to redis: ", exception);
            return;
        }
        throw exception;
    }
}
