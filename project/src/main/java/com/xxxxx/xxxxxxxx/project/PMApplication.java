/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/8 16:40
 */

package com.xxxxx.xxxxxxxx.project;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ImportResource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.stream.Collectors;

/*
 * PM means Project Management
 */

@MapperScan("com.xxxxx.xxxxxxxx.project.mapper")
@ServletComponentScan
@SpringBootApplication(scanBasePackages = {
        "com.xxxxx.xxxxxxxx.templateengine",
        "com.xxxxx.xxxxxxxx.templateengine.models",
        "com.xxxxx.xxxxxxxx.project",
        "com.xxxxx.xxxxxxxx.file",
        "com.xxxxx.xxxxxxxx.serviceadapter.scagent"
})
@ImportResource(locations = {"classpath:sc.xml"})
@EnableRetry
@EnableCaching
@EnableScheduling
public class PMApplication implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(PMApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PMApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        String strArgs = Arrays.stream(args.getSourceArgs()).collect(Collectors.joining("|"));
        logger.info("Application started with arguments:" + strArgs);
    }
}
