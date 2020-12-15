/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/12/08 11:37
 */

package com.xxxxx.xxxxxxxx.project.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfigurer {
    public static final String kDataSourceNameOffline = "DataSource-offline";

    @Primary
    @Bean
    @ConfigurationProperties(
            prefix = "spring.datasource.hikari-online"
    )
    public DataSource onlineDataSource(DataSourceProperties properties) {
        HikariDataSource dataSource =
                properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (!StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName("HikariPool-online");
        }
        return dataSource;
    }

    @Bean(kDataSourceNameOffline)
    @ConfigurationProperties(
            prefix = "spring.datasource.hikari-offline"
    )
    public DataSource offlineDataSource(DataSourceProperties properties) {
        HikariDataSource dataSource =
                properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (!StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName("HikariPool-offline");
        }
        return dataSource;
    }
}
