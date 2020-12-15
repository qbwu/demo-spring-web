/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/29 13:44
 */

package com.xxxxx.xxxxxxxx.project.cache;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;


public class RequestScopedCacheManager extends ConcurrentMapCacheManager implements BeanClassLoaderAware {
    @Nullable
    private SerializationDelegate serialization;

    public RequestScopedCacheManager() {
    }

    public RequestScopedCacheManager(String... cacheNames) {
        super(cacheNames);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        super.setBeanClassLoader(classLoader);
        this.serialization = new SerializationDelegate(classLoader);
    }

    @Override
    protected Cache createConcurrentMapCache(String name) {
        SerializationDelegate actualSerialization = super.isStoreByValue() ? this.serialization : null;
        return new RequestScopedCache(name, this.isAllowNullValues(), actualSerialization);
    }
}
