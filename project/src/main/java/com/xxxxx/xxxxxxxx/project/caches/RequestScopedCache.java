/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/29 11:23
 */

package com.xxxxx.xxxxxxxx.project.caches;

import com.xxxxx.xxxxxxxx.project.utility.ConfigrableBeanFactoryHolder;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.lang.Nullable;
import org.springframework.core.serializer.support.SerializationDelegate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestScopedCache extends ConcurrentMapCache {

    public RequestScopedCache(String name) {
        this(name, true);
    }

    public RequestScopedCache(String name, boolean allowNullValues) {
        this(name, allowNullValues, null);
    }

    protected RequestScopedCache(String name, boolean allowNullValues,
                                 @Nullable SerializationDelegate serialization) {
        super(name, getCacheStore(name), allowNullValues, serialization);
    }

    // Dynamically create the aop-proxied objects, just like the annotation
    // representation: @Scope("request", proxyMode = ScopedProxyMode.TARGET_CLASS).
    // But If we simply use the annotation above, all the cache stores(identified by
    // the cache names) will share a singleton ConcurrentMap in the scope of one
    // request, because they share the same proxy object and which targets to
    // the only actual bean definition, whereas we need separate maps for each cache store.
    private static ConcurrentMap<Object, Object> getCacheStore(String storeName) {
        registerCacheStoreTargetBean(storeName);
        // create the scoped proxy object of the target store
        ScopedProxyFactoryBean proxyBean = new ScopedProxyFactoryBean();
        proxyBean.setTargetBeanName(storeName);
        proxyBean.setProxyTargetClass(true);
        proxyBean.setBeanFactory(ConfigrableBeanFactoryHolder.getFactory());
        return (ConcurrentMap<Object, Object>) proxyBean.getObject();
    }

    private static void registerCacheStoreTargetBean(String storeName) {
        BeanDefinitionRegistry registry = ((BeanDefinitionRegistry)
                ConfigrableBeanFactoryHolder.getFactory());

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ConcurrentHashMap.class);
        beanDefinition.setLazyInit(true);
        beanDefinition.setAbstract(false);
        beanDefinition.setAutowireCandidate(false);
        beanDefinition.setScope("request");

        registry.registerBeanDefinition(storeName, beanDefinition);
    }
}
