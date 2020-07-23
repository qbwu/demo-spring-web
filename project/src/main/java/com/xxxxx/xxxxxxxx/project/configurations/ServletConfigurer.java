/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/26 22:27
 */

package com.xxxxx.xxxxxxxx.project.configurations;

import com.xxxxx.xxxxxxxx.file.interceptor.AkskInterceptor;
import com.xxxxx.xxxxxxxx.project.intercepters.LoginInterceptor;
import com.xxxxx.xxxxxxxx.project.intercepters.RequestLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Component
public class ServletConfigurer implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    AkskInterceptor akskInterceptor;

    @Autowired
    RequestLogInterceptor requestLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLogInterceptor).order(1);
        registry.addInterceptor(loginInterceptor).addPathPatterns("/xxxxxxxx/api/project/v1/**").order(2);
        registry.addInterceptor(akskInterceptor).addPathPatterns("/xxxxxxxx/api/project/inner/**").order(3);
        // There are many configurable build-in interceptors
    }
    // If you search on internet, most of people say to use RequestContextFilter with threadInheritable,
    // which is wrong, the FrameworkServlet will call its initContextHolders after the filter chain, so
    // and by default it will override the threadInheritability set by the RequestContextFilter.
    @Bean
    DispatcherServlet dispatcherServlet() {
        DispatcherServlet srvl = new DispatcherServlet();
        srvl.setThreadContextInheritable(true);
        return srvl;
    }
}
