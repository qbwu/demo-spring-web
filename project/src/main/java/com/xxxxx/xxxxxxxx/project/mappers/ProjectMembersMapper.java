/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/20 14:48
 */

package com.xxxxx.xxxxxxxx.project.mappers;

import com.xxxxx.xxxxxxxx.project.models.data.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProjectMembersMapper {
    @Select("<script>"
                + "SELECT `project_id` FROM xxxxxxx WHERE rel=#{rel} AND"
                + "<foreach collection=\"mems\" item=\"mem\" separator=\"OR\" open=\"(\" close=\")\">"
                    + "<choose>"
                        + "<when test=\"mem.userId!=null and mem.corpId==null\">"
                            + "user_id=#{mem.userId}"
                        + "</when>"
                        + "<when test=\"mem.userId==null and mem.corpId!=null\">"
                            + "corp_id=#{mem.corpId}"
                        + "</when>"
                        + "<when test=\"mem.userId!=null and mem.corpId!=null\">"
                            + "(user_id=#{mem.userId} AND corp_id=#{mem.corpId})"
                        + "</when>"
                    + "</choose>"
                + "</foreach>"
                + "<if test=\"lim!=null\">"
                    + "AND project_id IN"
                    + "<foreach collection=\"lim\" item=\"id\" separator=\",\" open=\"(\" close=\")\">"
                        + "#{id}"
                    + "</foreach>"
                + "</if>"
            + "</script>")
    List<String> findProjectsOfUsers(@Param("mems") Set<User> mems, String rel,
                                     @Param("lim") Set<String> limit);
}
