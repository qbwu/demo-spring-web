/***************************************************************************
 *
 * Copyright (c) 2020 qbwu, Inc. All Rights Reserved
 *
 **************************************************************************/

/**
 * @file NotificationSender.java
 * @author qb.wu@outlook.com 
 * @date 2020/05/22
 * @brief
 *
 **/

package com.xxxxx.xxxxxxxx.project.manager;

import com.xxxxx.xxxxxxxx.notification.receiver.dto.ButtonDTO;
import com.xxxxx.xxxxxxxx.notification.receiver.dto.IdDTO;
import com.xxxxx.xxxxxxxx.notification.receiver.dto.NotificationDTO;
import com.xxxxx.xxxxxxxx.notification.receiver.dto.ServiceNoticeDTO;
import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.NotificationContent;
import com.xxxxx.xxxxxxxx.notification.client.service.NotificationSrvClient;
import com.xxxxx.xxxxxxxx.serviceadapter.scagent.service.CorpSrvClient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ComponentScan(value = {"com.xxxxx.xxxxxxxx.serviceadapter.scagent.service",
        "com.xxxxx.xxxxxxxx.notification.client.service"},
        useDefaultFilters = false, includeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {CorpSrvClient.class, NotificationSrvClient.class})})
@PropertySource("classpath:application.properties")
public class NotificationSender {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSender.class);

    @Autowired
    private NotificationSrvClient notificationSrvClient;

    @Autowired
    private CorpSrvClient corpSrvClient;

    @Value("${notification.project-detail-url-tmpl}")
    private String gotoUrlTmpl;

    public boolean sendNotification(
            final User from,
            final List<String> to,
            final String projectId,
            final String projectName,
            final NotificationContent notificationContent) {
        // Do not notify self
        to.remove(from.getUserId());
        if (to.isEmpty()) {
            return true;
        }
        NotificationDTO notification = generateNotification(from, to, projectId, projectName,
                notificationContent);
        if (notification == null || this.notificationSrvClient.create(notification) == null) {
            log.error("Failed to send notification for project {}", projectId);
            return false;
        }
        return true;
    }

    private NotificationDTO generateNotification(
            final User from,
            final List<String> to,
            final String projectId,
            final String projectName,
            final NotificationContent notificationContent) {
        // add content
        NotificationDTO notification = new NotificationDTO();

        return notification;
    }
}
