/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/9/15 14:30
 */

package com.xxxxx.xxxxxxxx.project.mapper;

import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.enums.ProjectUserRel;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

// TODO fix it to configurable if we can some day...
// Unfortunately, these parameters must be compiled-time constants, and
// cannot be place-holders of configuration items by now.

// Flush the cache every 30 seconds to achieve an acceptable consistency;
// hold at most 100 cache records;
@CacheNamespace(flushInterval = 30000, size = 100, readWrite = false)
@Repository
public interface CorpProjectsMapper {

    @Select("<script>"
            + "SELECT `project_id` FROM project_members WHERE rel IN "
                + "<foreach collection=\"rels\" item=\"rel\" separator=\",\" open=\"(\" close=\")\">"
                    + "#{rel}"
                + "</foreach>"
                + " AND "
                + "<foreach collection=\"mems\" item=\"mem\" separator=\"OR\" open=\"(\" close=\")\">"
                    + "corp_id=#{mem.corpId} "
                + "</foreach>"
            + "</script>")
    List<String> findProjectsOfCorps(@Param("mems") Set<User> mems, Set<ProjectUserRel> rels);
}
