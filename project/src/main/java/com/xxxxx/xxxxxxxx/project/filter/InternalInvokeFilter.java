/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/8/4 19:27
 */

package com.xxxxx.xxxxxxxx.project.filter;

import com.xxxxx.xxxxxxxx.project.model.RequestContext;
import com.xxxxx.xxxxxxxx.project.model.enums.ServiceScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

// Interceptors are called after request handlers being determined by the servlet path,
// so we cannot use them to overwrite the internal URI.
@Component
public class InternalInvokeFilter implements Filter {
    @Autowired
    private RequestContext context;

    private static final String basePath = "/xxxxxxxxxxx";
    private static final String internalBasePath = basePath + "/xxx";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        if (!httpServletRequest.getServletPath().startsWith(internalBasePath)) {
            context.setServiceScope(ServiceScope.CLIENT);
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        context.setServiceScope(ServiceScope.INTERNAL);

        filterChain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
            // Don't overwrite the getRequestURI, firstly it do nothing with the
            // request mapping, secondly we need to log the URI in the
            // RequestLogInterceptor
            @Override
            public String getServletPath() {
                return basePath + httpServletRequest.getServletPath().substring(internalBasePath.length());
            }
        }, servletResponse);
    }
}
