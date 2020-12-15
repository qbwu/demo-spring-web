/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/25 20:51
 */

package com.xxxxx.xxxxxxxx.project.intercepter;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xxxxx.xxxxxxxx.project.controller.PMController;
import com.xxxxx.xxxxxxxx.project.utility.RequestLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import com.xxxxx.xxxxxxxx.project.library.hi.AuthInfo;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    RequestLogger requestLogger;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        request.setAttribute(PMController.getKHiAuth(), new AuthInfo(null, null));

        return true;
    }
}
