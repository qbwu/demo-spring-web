/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/12/03 14:10
 */

package com.xxxxx.xxxxxxxx.project.service.offline;

import com.xxxxx.driver4j.bns.StringUtils;
import com.xxxxx.xxxxxxxx.project.manager.NotificationSender;
import com.xxxxx.xxxxxxxx.project.mapper.ProjectMembersMapper;
import com.xxxxx.xxxxxxxx.project.mapper.AppKeyValueMapper;
import com.xxxxx.xxxxxxxx.project.model.NotificationContent;
import com.xxxxx.xxxxxxxx.project.model.Protocol.Request;
import com.xxxxx.xxxxxxxx.project.model.Protocol.ResponseData;
import com.xxxxx.xxxxxxxx.project.model.Protocol.SetAttrValueRequest;
import com.xxxxx.xxxxxxxx.project.model.Protocol.SetAttrValueResponse;
import com.xxxxx.xxxxxxxx.project.model.RequestContext;
import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.enums.ProjectUserRel;
import com.xxxxx.xxxxxxxx.project.model.enums.ServiceScope;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.exception.StaticException;
import com.xxxxx.xxxxxxxx.project.model.template.ExtValueTemplateAttr;
import com.xxxxx.xxxxxxxx.project.model.template.ProjectFollowersAttr;
import com.xxxxx.xxxxxxxx.project.model.template.Templatable;
import com.xxxxx.xxxxxxxx.serviceadapter.scagent.service.CorpSrvClient;
import com.xxxxx.xxxxxxxx.templateengine.models.TemplateAttrManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DismissalJobService {
    private static final Logger logger = LoggerFactory.getLogger(DismissalJobService.class);

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static final String kDismissalJobVersion = "project.xxx";

    @Value("${offline.dismissal.query.employee.batch.size:#{100}}")
    private Long queryEmployeeBatchSize;

    // For testing
    @Value("${offline.dismissal.process.employee.num:#{-1}}")
    private Long processEmployeeNum;

    @Autowired
    private CorpSrvClient corpSrvClient;

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private TemplateAttrManager teManager;

    @Autowired
    private ProjectMembersMapper dao;

    @Resource
    private DismissalJobService self;

    private final SqlSessionFactory sqlSessionFactory;
    private Set<User> dismissedEmployees = Collections.emptySet();

    @Autowired
    public DismissalJobService(@Qualifier(kDataSourceNameOffline) DataSource dataSource) throws Exception {
        // We have to manage the sqlSession manually, because the release_lock must use
        // the session from which the lock was granted, and the job is a long-term one,
        // the max life time of the low-level connection must be configured properly.
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        sqlSessionFactory = factoryBean.getObject();
        assert sqlSessionFactory != null;
        sqlSessionFactory.getConfiguration().addMapper(AppKeyValueMapper.class);
    }

    @Scheduled(cron = "${offline.dismissal.job.schedule.cron:-}")
    public void process() {
        String thisDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

        try (SqlSession sqlSession = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE)) {
            String version = acquireVersion(sqlSession);
            if (version.compareTo(thisDate) >= 0) {
                logger.info("Relinquished execution of the job at date: {}", version);
                return;
            }

            doWorks();

            releaseVersion(sqlSession, thisDate);
            logger.info("Finished the job at date: {}", thisDate);
        } catch (Exception ex) {
            logger.error("Failed to run the job at: {}, reason: ", thisDate, ex);
        }
    }

    private String acquireVersion(SqlSession sqlSession) {
        AppKeyValueMapper mapper = sqlSession.getMapper(AppKeyValueMapper.class);
        while (mapper.acquireLock(kDismissalJobVersion + "-Lock", 0) != 1) {
            sqlSession.commit();
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
            }
        }
        String version = mapper.getValue(kDismissalJobVersion);
        sqlSession.commit();
        return version;
    }

    private void releaseVersion(SqlSession sqlSession, String version) {
        AppKeyValueMapper mapper = sqlSession.getMapper(AppKeyValueMapper.class);
        mapper.setValue(kDismissalJobVersion, version);
        if (mapper.releaseLock(kDismissalJobVersion + "-Lock") != 1) {
            logger.warn("Tried to release a non-possessed lock.");
        }
        sqlSession.commit();
    }

    private void doWorks() {

    }

    @CacheEvict(cacheManager = kCacheManagerNameRemote, cacheNames = {kCacheNameAttrs})
    public void evictCachedTemplatable(String id) {

    }
}
