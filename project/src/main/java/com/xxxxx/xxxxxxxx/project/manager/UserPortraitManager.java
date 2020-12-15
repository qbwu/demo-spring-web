/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/4 11:55
 */

package com.xxxxx.xxxxxxxx.project.manager;

import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.serviceadapter.scagent.service.CorpSrvClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xxxxx.xxxxxxxx.project.configuration.CacheConfigurer.kCacheManagerNameRequestScoped;
import static com.xxxxx.xxxxxxxx.project.configuration.CacheConfigurer.kCacheNameUserPortrait;

@Service
@CacheConfig(cacheManager = kCacheManagerNameRequestScoped,
             cacheNames = kCacheNameUserPortrait)
public class UserPortraitManager {
    private final static Logger logger = LoggerFactory.getLogger(UserPortraitManager.class);
    public final static String kPortrait = "portrait";
    public final static String kLeaveFlag = "leave";

    @Autowired
    CorpSrvClient corpSrvClient;

    // Self-autowired reference to proxied bean of this class.
    @Resource
    private UserPortraitManager self;

    public Map<String, Map<String, String>> getUserPortraitBatch(List<User> users) {
        if (users.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> cacheMissUsers = new ArrayList<>();
        Map<String, Map<String, String>> res = new HashMap<>();
        for (User user : users) {
            Map<String, String> cached = this.self.getUserPortraitFromCache(user);
            if (cached != null) {
                res.put(user.getUserId(), cached);
            } else if (UserPermManager.getAdminUserId().equals(user.getUserId())) {
                // Ignore the super user infoflow / infoflow
                res.put(user.getUserId(), Collections.emptyMap());
            } else {
                cacheMissUsers.add(user);
            }
        }
        if (cacheMissUsers.isEmpty()) {
            return res;
        }
        try {
            Map<String, Map<String, String>> queried = corpSrvClient.getBatchEmployeesInfoContainsLeave(
                    cacheMissUsers.get(0).getCorpId(),
                    cacheMissUsers.stream().map(User::getUserId).collect(Collectors.toList()));
            if (queried != null) {
                res.putAll(queried);
            } else {
                logger.warn("Got null user portrait batch");
            }
        } catch (Exception e) {
            logger.warn("Failed to get user portrait batch, {}", e.getMessage(), e);
        }
        for (User user : cacheMissUsers) {
            // Add empty map to cache if fail to get the employee info
            this.self.updateUserPortraitCache(user,
                    res.getOrDefault(user.getUserId(), Collections.emptyMap()));
        }
        return res;
    }

    @Cacheable
    public Map<String, String> getUserPortrait(User user) {
        logger.debug("Cache missed: {}", user.toString());
        try {
            if (UserPermManager.getAdminUserId().equals(user.getUserId())) {
                // Ignore the super user infoflow / infoflow
                return Collections.emptyMap();
            }
            Map<String, String> res = corpSrvClient.getEmployeeInfoContainsLeave(
                    user.getCorpId(), user.getUserId());
            if (res != null) {
                return res;
            }
            logger.warn("Got null user portrait, user: {}", user.toString());
        } catch (Exception e) {
            logger.warn("Failed to get user portrait, user: {}, {}",
                    user.toString(), e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    // Cache annotations can only be applied to public methods,
    // and only take effect in calls by proxy objects.
    @Cacheable(key = "#user", unless = "#result == null")
    public Map<String, String> getUserPortraitFromCache(User user) {
        logger.debug("Cache missed: {}", user.toString());
        return null;
    }

    @CachePut(key = "#user")
    public Map<String, String> updateUserPortraitCache(User user,
                                                       Map<String, String> portrait) {
        logger.debug("Cache updated: {}->username={}",
                     user.toString(), portrait.get("userId"));
        return portrait;
    }
}
