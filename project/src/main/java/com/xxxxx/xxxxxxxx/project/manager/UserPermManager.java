/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/21 14:46
 */

package com.xxxxx.xxxxxxxx.project.manager;

import com.xxxxx.xxxxxxxx.project.mapper.AssociationMapper;
import com.xxxxx.xxxxxxxx.project.mapper.CorpProjectsMapper;
import com.xxxxx.xxxxxxxx.project.mapper.ProjectMembersMapper;
import com.xxxxx.xxxxxxxx.project.model.Protocol.Request;
import com.xxxxx.xxxxxxxx.project.model.Protocol.StatusCode;
import com.xxxxx.xxxxxxxx.project.model.RequestContext;
import com.xxxxx.xxxxxxxx.project.model.enums.ProjectUserRel;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.data.TemplatableAssociation;
import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.exception.RequestParamException;
import com.xxxxx.xxxxxxxx.project.model.template.AttrConfig;
import com.xxxxx.xxxxxxxx.project.utility.framework.SpringApplicationContextHolder;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserPermManager {

    private final RequestContext requestContext;
    private final ProjectMembersMapper projectMemberDao;
    private final AssociationMapper associationDao;
    private final CorpProjectsMapper corpProjectsDao;

    @Autowired
    UserPermManager(RequestContext requestContext,
                    ProjectMembersMapper projectMemberDao,
                    AssociationMapper associationDao,
                    CorpProjectsMapper corpProjectsDao) {
        this.requestContext = requestContext;
        this.projectMemberDao = projectMemberDao;
        this.associationDao = associationDao;
        this.corpProjectsDao = corpProjectsDao;
    }

    // Necessary for injecting static fields.
    @Value("${service.permission.user.admin.userId}")
    void setAdminUserId(String userId) {
        adminUserId = userId;
    }

    @Value("${service.permission.user.admin.corpId}")
    void setAdminCoprId(String corpId) { adminCorpId = corpId; }

    public static Boolean isAdministrator(String userId) {
        return adminUserId.equals(userId);
    }

    private void errorNoPermission(String permType, TemplateType objType) {
        requestContext.setStatus(HttpStatus.FORBIDDEN, StatusCode.NoPermission);
        throw new RequestParamException(String.format(
                "Have no %s permission to access the %s", permType, objType));
    }
}
