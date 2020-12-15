/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/29 22:42
 */

package com.xxxxx.xxxxxxxx.project.utility.framework;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class ConfigrableBeanFactoryHolder implements BeanFactoryPostProcessor {
    private static ConfigurableListableBeanFactory factory;

    public static ConfigurableListableBeanFactory getFactory() {
        return factory;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory)
            throws BeansException {
        this.factory = factory;
    }
}
