/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/12/08 21:32
 */

package com.xxxxx.xxxxxxxx.project.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface AppKeyValueMapper {
    @Select("SELECT GET_LOCK(#{lockName}, #{timeout})")
    Integer acquireLock(String lockName, int timeout);

    @Select("SELECT RELEASE_LOCK(#{lockName})")
    Integer releaseLock(String lockName);

    @Select("SELECT value FROM application_kv_store WHERE name=#{key}")
    String getValue(String key);

    @Update("UPDATE application_kv_store SET value=#{value} WHERE name=#{key}")
    void setValue(String key, String value);
}
