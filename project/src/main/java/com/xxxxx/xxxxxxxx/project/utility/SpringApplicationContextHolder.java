/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/20 20:31
 */

package com.xxxxx.xxxxxxxx.project.utility;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationContextHolder implements ApplicationContextAware {
    private static ApplicationContext context;

    public static ApplicationContext getContext() { return context; }

    public static Object getMaybeTargetObject(Object proxy) throws Exception {
        if (proxy instanceof Advised && AopUtils.isAopProxy(proxy)) {
            return ((Advised) proxy).getTargetSource().getTarget();
        } else {
            return proxy;
        }
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

}
