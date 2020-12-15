/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/9/11 21:43
 */

package com.xxxxx.xxxxxxxx.project.cache.serializer;

import com.xxxxx.xxxxxxxx.project.utility.framework.ConfigrableBeanFactoryHolder;
import com.xxxxx.xxxxxxxx.project.utility.framework.SpringApplicationContextHolder;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;

public class AopProxySerializer<T> extends AutowireSerializer<T> {
    private static final Logger logger = LoggerFactory.getLogger(AopProxySerializer.class);

    public AopProxySerializer(Kryo kryo, Class type) {
        super(kryo, type);
    }

    @Override
    public void write(Kryo kryo, Output output, T t) {
        // Extract target object to write, because the CGLIB classes enhanced by Spring
        // cannot be serialized.
        T target;
        try {
            target = (T) SpringApplicationContextHolder.getMaybeTargetObject(t);
        } catch (Exception e) {
            logger.error("Failed write target of the proxy, reason: {}",
                    e.getMessage(), e);
            return;
        }

        super.write(kryo, output, target);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> aClass) {
        T target = super.read(kryo, input, aClass);
        // Wrap the target with the CGLIB proxy back
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addInterface(AopInfrastructureBean.class);
        return (T) proxyFactory.getProxy(
                ConfigrableBeanFactoryHolder.getFactory().getBeanClassLoader());
    }
}
