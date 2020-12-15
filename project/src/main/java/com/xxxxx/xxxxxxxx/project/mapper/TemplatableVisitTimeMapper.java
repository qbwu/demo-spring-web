/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/7/27 19:11
 */

package com.xxxxx.xxxxxxxx.project.mapper;

import com.xxxxx.xxxxxxxx.project.model.data.DateRange;
import com.xxxxx.xxxxxxxx.project.model.data.TemplatableVisitor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TemplatableVisitTimeMapper {

    @Select("SELECT `visit_time` FROM object_visit_time "
                + "WHERE id=#{id} AND user_id=#{user.userId} AND corp_id=#{user.corpId} "
                + "AND object_type=#{objectType} AND visit_type=#{visitType}")
    Long getVisitTime(TemplatableVisitor visitor);

    @Select("<script>"
                + "SELECT MAX(`visit_time`) FROM object_visit_time "
                    + "WHERE id=#{id} AND object_type=#{objectType} "
                    + "<if test=\"user!=null\">"
                        + "AND user_id=#{user.userId} AND corp_id=#{user.corpId} "
                    + "</if>"
                    + "<if test=\"visitType!=null\">"
                        + "AND visit_type=#{visitType} "
                    + "</if>"
            + "</script>")
    Long getMaxVisitTime(TemplatableVisitor visitor);

    @Insert("REPLACE INTO object_visit_time(`id`, `user_id`, `corp_id`, `object_type`, `visit_type`, `visit_time`) "
                + "VALUES (#{vi.id}, #{vi.user.userId}, #{vi.user.corpId}, #{vi.objectType}, #{vi.visitType}, #{time})")
    void putVisitTime(@Param("vi") TemplatableVisitor visitor, @Param("time") Long time);

    @Select("<script>"
                + "SELECT `id` FROM object_visit_time "
                    + "WHERE object_type=#{vi.objectType} AND visit_type=#{vi.visitType} "
                        + "AND user_id=#{vi.user.userId} AND corp_id=#{vi.user.corpId} "
                        + "<if test=\"per.startTime!=null\">"
                            + "AND visit_time &gt;= #{per.startTime}"
                        + "</if>"
                        + "<if test=\"per.endTime!=null\">"
                            + "AND visit_time &lt; #{per.endTime}"
                        + "</if>"
                        + "<if test=\"lim!=null\">"
                            + "AND id IN"
                            + "<foreach collection=\"lim\" item=\"id\" separator=\",\" open=\"(\" close=\")\">"
                                + "#{id}"
                            + "</foreach>"
                        + "</if>"
            + "</script>")
    List<String> findObjectsByVisitTime(@Param("vi") TemplatableVisitor visitor,
                                        @Param("per") DateRange period,
                                        @Param("lim") Set<String> limit);

    @Select("<script>"
                + "SELECT `id` FROM object_visit_time "
                    + "WHERE object_type=#{vi.objectType} "
                    + "<if test=\"vi.user!=null\">"
                        + "AND user_id=#{vi.user.userId} AND corp_id=#{vi.user.corpId} "
                    + "</if>"
                    + "<if test=\"vi.visitType!=null\">"
                        + "AND visit_type=#{vi.visitType} "
                    + "</if>"
                    + "<if test=\"lim!=null\">"
                        + "AND id IN "
                        + "<foreach collection=\"lim\" item=\"id\" separator=\",\" open=\"(\" close=\")\">"
                            + "#{id}"
                        + "</foreach>"
                    + "</if>"
                    + "GROUP BY id"
                    + "<trim prefix=\"HAVING\" suffixOverrides=\"AND\">"
                        + "<if test=\"per.startTime!=null\">"
                            + "MAX(visit_time) &gt;= #{per.startTime} AND "
                        + "</if>"
                        + "<if test=\"per.endTime!=null\">"
                            + "MAX(visit_time) &lt; #{per.endTime} AND "
                        + "</if>"
                    + "</trim>"
            + "</script>")
    List<String> findObjectsByMaxVisitTime(@Param("vi") TemplatableVisitor visitor,
                                           @Param("per") DateRange period,
                                           @Param("lim") Set<String> limit);

}
