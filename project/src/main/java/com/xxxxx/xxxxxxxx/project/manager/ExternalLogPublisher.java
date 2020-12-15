/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/11/05 20:55
 */

package com.xxxxx.xxxxxxxx.project.manager;

import com.xxxxx.xxxxxxxx.project.model.Protocol.IdAttr;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.exception.StaticException;
import com.xxxxx.xxxxxxxx.project.model.template.Templatable;
import com.xxxxx.xxxxxxxx.project.utility.HttpAsyncClientUtils;
import com.xxxxx.xxxxxxxx.project.utility.InheritableRequestContextTaskWrapper;
import com.xxxxx.xxxxxxxx.project.utility.ObjectMapUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang.ObjectUtils;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ExternalLogPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ExternalLogPublisher.class);

    @Value("${service.logsink.kgflow.url}")
    private String kgFlowLogsinkUrl;
    @Value("${service.logsink.kgflow.sourceId}")
    private String kgFlowLogsinkSrcId;
    @Value("${service.logsink.kgflow.appKey}")
    private String kgFlowLogsinkAK;
    @Value("${service.logsink.kgflow.secretKey}")
    private String kgFlowLogsinkSK;

    private final HttpAsyncClientUtils httpAsyncClientUtils;

    public enum Operation { Update, Delete };

    @Autowired
    public ExternalLogPublisher(HttpAsyncClientUtils httpAsyncClientUtils) {
        this.httpAsyncClientUtils = httpAsyncClientUtils;
    }

    public void postChangeLog(Operation action, Templatable templatable) {
        try {
            if (!ObjectUtils.equals(templatable.getRequestContext().getTemplateType(),
                    TemplateType.project)) {
                return;
            }

            Map<String, Object> projectAttrValues = getProjectAttrValues(templatable);

            Supplier<Map<String, Object>> jsonProducer = () -> {
                Map<String, Object> jsonData = ObjectMapUtils.objectToMap(
                        new IdAttr(templatable.getId(), projectAttrValues));

                jsonData.put("action", action == Operation.Update ? "update" : "delete");
                return new HashMap<String, Object>() {};
            };

            httpAsyncClientUtils.httpPostJson(kgFlowLogsinkUrl, jsonProducer, null,
                new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse result) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Completed posting project change log, statusCode={}",
                                    result.getStatusLine().getStatusCode());
                        }
                    }
                    // Notice that the exceptions from jsonProducer are also handled here
                    @Override
                    public void failed(Exception ex) {
                        logger.warn("Failed to post project change log, reason: ", ex);
                    }

                    @Override
                    public void cancelled() {

                    }
                });
        } catch (Exception ex) {
            logger.warn("Failed to push log data to the collector, reason: ", ex);
        }
    }

    @SneakyThrows
    private Map<String, Object> getProjectAttrValues(Templatable project) {
    }
}
